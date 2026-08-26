# Cloud Agent Requirements for Android/Kotlin CI Independence

This document is for whoever preps this repo for cloud-sandboxed AI agents (Claude Code on
the web, or similar remote-execution agents) — what has to be true of the repo, its CI, and
its process conventions so a cloud agent can drive an Android/Kotlin change end-to-end
(diagnose → fix → validate → merge) **without a handoff back to a human on a local machine**.

It's written from direct experience driving PR #6 (the Maestro→Robolectric/Roborazzi CI
migration) as a cloud agent against this exact repo — every capability and limitation below
was actually exercised, not assumed. Where something is sandbox/session-specific rather than
a universal cloud-agent constraint, it's marked as such.

## Why this matters

A cloud agent's sandbox is not a dev machine. It has git, a shell, a text editor, and network
access mediated by an egress policy proxy — but no Android SDK, no JDK, no emulator, no
display, and no guarantee that every external host is reachable. Confirmed in this session:
`which adb emulator maestro java` all returned nothing. That's not a bug to route around —
it's the operating assumption every piece of prep below is designed for. **The GitHub Actions
runners are the cloud agent's only real Android toolchain.** Everything the agent does has to
route through triggering CI, reading CI's output, and reacting — never through running Gradle,
adb, or an emulator itself.

## Confirmed cloud-sandbox capabilities (baseline — don't re-verify these each session)

- Git clone/checkout/commit/push over HTTPS to GitHub.
- Full GitHub API access (issues, PRs, reviews, comments, commits, file contents, checks,
  Actions run/job listing, job log retrieval, triggering `workflow_dispatch`, re-running failed
  jobs, merging PRs) — confirmed working end-to-end in this session via the GitHub MCP tool
  surface, including creating and merging PRs, re-running a failed CI job, and reading full job
  logs (large logs get saved to a local file and need chunked/`grep`-based reading — a tool
  ergonomics detail, not a capability gap).
- Reading and writing arbitrary repo files, running local scripts (Python, shell) that don't
  need Android/JVM tooling.
- Outbound HTTPS to major public package registries — **confirmed reachable this session**:
  Maven Central (`repo1.maven.org`), the Gradle Plugin Portal (`plugins.gradle.org`), GitHub
  itself. Used live to verify a real, currently-published dependency version (Roborazzi
  1.73.0) before committing to it, rather than guessing a version and letting CI discover the
  mistake.

## Confirmed cloud-sandbox limitations (design around these — don't try to work past them)

- **No Android SDK, JDK, Gradle, adb, or emulator.** The agent cannot run `./gradlew`, install
  an APK, or drive a real or emulated device. Any change to Kotlin/Compose code is validated
  entirely by pushing and reading back CI's result — there is no local "does it compile" check
  before that push. (See "CI as the sole build/test executor" below for what this requires of
  CI turnaround and diagnostics.)
- **No display / no interactive tooling.** Anything that expects a human to click through a UI
  (`maestro studio`'s live inspector, an interactive device picker) is categorically out of
  scope for a cloud agent, not just currently unimplemented. Don't design a workflow that
  assumes a cloud agent can ever do this step — mark it explicitly human/local-only.
- **Binary CI artifact downloads (e.g. `actions/upload-artifact`) can be blocked at the
  session's network-egress-policy level**, independent of anything the repo controls. Confirmed
  this session: GitHub's artifact-download flow redirects to Azure Blob Storage
  (`*.blob.core.windows.net`), and that specific host was denied by this session's egress
  policy (`403` on the CONNECT tunnel) even though `github.com`/`api.github.com`, Maven
  Central, and the Gradle Plugin Portal were all reachable. This is an org/session-level
  policy decision, not something a repo-level prep step can fix — the local/repo-owning side
  of this is to (a) not build a workflow that *requires* an agent to fetch a binary artifact to
  do its job (see next section), and (b) if broader artifact-host access is genuinely needed,
  escalate to whoever administers the Claude Code deployment's egress policy rather than trying
  to route around it from inside the sandbox.
- **No access to CI secrets.** The agent never sees `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, or any
  other GitHub Actions secret, and shouldn't need to — release signing has to stay entirely
  inside CI. This is already correctly the case in this repo (`ci.yml`'s
  `build-release-apk` job); no change needed, just don't regress it by ever plumbing a secret
  into something a cloud agent's steps would need to read directly.

## Required local/repo-owner preparation

### 1. GitHub App/connector scope on the repo

The account or GitHub App the cloud agent authenticates as needs, at minimum: `contents:
write` (push branches/commits), `pull_requests: write` (open/update/merge PRs),
`checks: read` and `actions: read+write` (read CI status/logs, trigger `workflow_dispatch`,
re-run failed jobs), `issues: write` (PR comments). All of this worked in this session against
this repo — confirm it's still true for any new repo before assuming a cloud agent can operate
on it end-to-end; a repo not enabled for the agent's GitHub connector fails silently as "can't
see this repo" rather than a clear permissions error.

### 2. CI as the sole build/test executor — make sure it's actually reachable without a human

- Every check that gates a merge (lint, unit tests, instrumented/emulator tests, APK assembly)
  must run on `push`/`pull_request`, or be triggerable by the agent via `workflow_dispatch`
  without requiring a manual "approve this workflow run" click (GitHub gates first-time/outside
  contributor workflow runs behind approval by default — make sure the agent's identity is
  trusted enough on this repo that this gate doesn't apply, or the agent will stall waiting for
  a human it has no way to page).
- Branch protection on the merge-target branch should require the CI checks to pass (good — a
  cloud agent should never merge past red CI) but should **not** require a human-only review
  approval that the agent's own account can't satisfy, unless the intent is genuinely
  "cloud agent proposes, human always merges" — decide which model this repo wants and set
  branch protection to match, rather than leaving it ambiguous. (This repo's actual branch
  protection state on `main` wasn't checked as part of this session — verify it explicitly
  rather than assuming.)

### 3. Design CI failure output to be diagnosable from logs alone, not from downloaded artifacts

Given the Azure Blob artifact-download limitation above, treat **the job's own stdout/stderr
log as the primary diagnostic channel**, and artifact uploads as a secondary channel for a
human who can download them:

- Print human-readable pass/fail summaries directly to the console, not just to a file that
  only exists inside a zipped artifact. This repo's Maestro runner already did this well —
  `[Failed] 01_app_launch_and_theme_audit (1m 36s) (Assertion is false: id: app_title is
  visible)` in the raw log was enough to diagnose from, with zero artifact download needed.
  Roborazzi/JUnit/Detekt's default console output is similarly sufficient — no change needed
  there, just don't regress it by redirecting that output only to a file.
- When a failure could stem from more than one cause, log enough context to disambiguate
  from the console alone — e.g. this session distinguished "the fix didn't work" from "an
  unrelated CI driver never started" purely by each flow's *reported duration* in the log
  (~3s = never ran at all; 1+ minutes = genuinely executed and asserted). If a future gate can
  fail in more than one distinguishable way, make sure timing/state info that disambiguates
  them is visible in plain log text.
- For anything that currently *only* exists as a binary artifact (screenshots, hierarchy
  dumps), consider also emitting a text/JSON summary to the console (e.g. a list of what was
  captured, dimensions, a hash) so an agent that can't download the zip still knows whether
  the step produced something and roughly what.

### 4. Give the cloud agent a way to produce toolchain-dependent baselines without local tooling

Some artifacts can only be generated by something with a real JVM/Android toolchain —
Roborazzi's golden screenshot baselines are the concrete example in this repo (`./gradlew
:app:recordRoborazziDebug`, see `RECIPES.md` §7A). A cloud agent structurally cannot run that
command itself. To keep this from becoming a mandatory human handoff every time a screen
legitimately changes:

- Add a dedicated `workflow_dispatch`-triggered CI job that runs the record task and **commits
  the resulting images back** (either directly to the branch, via a bot commit, or by opening a
  PR with just the updated goldens) — something the agent can trigger via
  `workflow_dispatch` and then wait on, the same way it already triggers/re-runs other jobs in
  this repo. This is the one concrete infra gap this session didn't close (no such job exists
  yet) — recording Tier 2's first baselines is still a manual, local-tooling step until this is
  added.
- Once that job exists, the record → commit → verify loop is fully agent-drivable: trigger
  record, wait for the commit, add `-Proborazzi.test.verify=true` to the blocking test step
  once satisfied.

### 5. Keep human/local-only steps explicitly and separately documented, not silently skipped

Not everything should become cloud-agent-drivable — `maestro studio`'s interactive inspector is
correctly local-only, and should stay that way. The failure mode to avoid isn't "a step
requires a human" — it's a cloud agent silently doing nothing about a step it can't perform, or
worse, guessing at a workaround (this session's aborted `maestro-runner` third-party-tool swap,
see `GOTCHAS.md` #17, is exactly the kind of workaround-under-pressure this prep is meant to
prevent). Two things make the boundary safe instead of a silent gap:
- Mark human-only steps as such in the same doc a cloud agent would read (`RECIPES.md`'s §7B
  already does this for Maestro), so an agent recognizes "not mine to do" instead of inventing
  a substitute.
- Give the agent an async channel to hand off exactly what it needs from a human/local step and
  what it found so far — this repo's pattern of writing findings into `GOTCHAS.md`/`RECIPES.md`
  and a PR comment naming the precise next local action (e.g. "run `maestro studio` and check
  whether `app_title` is visible in the live hierarchy") is a working example of this, not a
  theoretical recommendation.

### 6. Verify dependency versions live rather than trusting training-data recall

An agent's knowledge of "the latest version of X" is not live. Before committing a new
dependency/plugin version (especially for a fast-moving library), have the agent check the
real registry — this session verified Roborazzi's latest version via
`curl https://repo1.maven.org/maven2/.../maven-metadata.xml` and the equivalent Gradle Plugin
Portal metadata endpoint before writing it into `libs.versions.toml`, rather than guessing and
letting a CI failure discover a wrong or unpublished version number. No repo-side prep needed
for this beyond confirming Maven Central / the Gradle Plugin Portal stay reachable (see the
egress-policy note above) — it's a technique to expect from the agent, not a repo requirement.

## Self-check: is this repo actually cloud-agent-independent for Android/Kotlin work?

A local agent (or repo owner) can sanity-check the above by walking through this once:

1. Push a trivial, deliberately-broken commit (e.g. a syntax error in one Kotlin test file) on
   a throwaway branch and confirm every gating CI check runs automatically, no manual "approve
   workflow run" click required.
2. Confirm the resulting failure is fully diagnosable from the job's plain-text log alone (no
   artifact download needed) — read only the console output and see whether it's enough to
   know exactly what broke and where.
3. Confirm `workflow_dispatch` can be triggered against that branch via the GitHub API/App the
   agent will actually use (not just `gh` CLI as a human) — this is what lets an agent re-run a
   flake or fire an on-demand job like the Roborazzi baseline recorder in item 4 above.
4. Confirm the agent's account/App can open a PR, get it past required checks, and merge it
   without hitting a required-human-review gate it can't satisfy — or, if that gate is
   intentional policy, confirm the repo's docs say so explicitly rather than leaving an agent to
   discover it mid-task.
5. Confirm secrets never need to flow to the agent — grep the workflows for anything gated on a
   secret and make sure the agent's own steps never need to read it back.

If all five hold, a cloud agent can take an Android/Kotlin change from diagnosis to merged PR
without a human touching a local machine — everything else in this repo's process (draft PRs,
`GOTCHAS.md`/`RECIPES.md` as the async handoff channel for genuinely human-only steps, CI as
the only real build executor) was already built and proven out during PR #6 and needs no
further change to keep working this way.
