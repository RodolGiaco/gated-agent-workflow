# Hooks

The kit ships four hooks. Their source lives in [`kit/hooks/`](../kit/hooks), `kit/settings.json` registers them, and `kit/install.sh` copies them into `.claude/hooks/`, which is ignored by git and regenerated on every install.

## Contents

- [Registration](#registration)
- [How a guard reads a command](#how-a-guard-reads-a-command)
- [Decisions](#decisions)
- [guard-push.sh](#guard-pushsh)
- [guard-commit.sh](#guard-commitsh)
- [guard-merge.sh](#guard-mergesh)
- [Work branches](#work-branches)
- [session-context.sh](#session-contextsh)
- [Limits](#limits)
- [Adding a case](#adding-a-case)

## Registration

| Hook | Event | Runs for | Timeout |
|---|---|---|---|
| `guard-push.sh` | PreToolUse | Bash commands that match `Bash(git *)` | 10 s |
| `guard-commit.sh` | PreToolUse | Bash commands that match `Bash(git *)` | 10 s |
| `guard-merge.sh` | PreToolUse | Bash commands that match `Bash(gh *)` | 10 s |
| `session-context.sh` | SessionStart | Every session start: startup, resume and after compaction | 10 s |

The three guards share their helpers through `guard-common.sh`, which is sourced and never run on its own.

## How a guard reads a command

1. **Input.** `read_command` takes the hook payload from stdin and extracts `.tool_input.command` with jq.
2. **Branch.** The push and commit guards read the checked-out branch with `git symbolic-ref --short HEAD`, which also works on a branch with no commits yet, and only once the subcommand is known to be a push or a commit. Every other git command, read-only ones included, never needs a branch, so it keeps working from a detached HEAD such as a CI checkout. The merge guard never reads it.
3. **Tokens.** `tokenize` pads every shell separator (`&&`, `||`, `;`, `|`, `&`) with spaces, so a separator glued to a word, as in `main;`, stands on its own, and then splits the command on whitespace.
4. **Segments.** The guard walks the tokens one command segment at a time, resetting its state at each separator. At the start of a segment it skips `VAR=value` assignments and the wrappers `timeout`, `time`, `nice`, `nohup`, `stdbuf`, `command` and `builtin`, together with their numeric or flag arguments. The next word is the command word.
5. **Subcommand.** For `git`, the guard skips global options until the subcommand, consuming the argument of `-C`, `-c`, `--git-dir`, `--work-tree`, `--namespace` and `--exec-path`. A subcommand produced by an expansion is refused, because it hides which operation will run.
6. **Rules.** The guard applies its own rules to the arguments of that segment.

## Decisions

| Outcome | How the hook reports it |
|---|---|
| Allow | Exit 0 with no output |
| Refuse | Exit 0 with a `permissionDecision: deny` JSON on stdout. The reason reaches the model, and one line is appended to `.claude/logs/guard-denials.jsonl` |
| Cannot decide | Exit 2 with `HOOK ERROR [<guard>]: <reason>` on stderr, which blocks the call: missing jq, unreadable `kit.vars`, a missing required key, an empty command, or a commit or push from a detached HEAD |

Each line of the refusal log is a JSON object with `at`, `guard`, `branch`, `command` and `reason`.

## guard-push.sh

For every `git push` segment:

| Condition | Decision |
|---|---|
| The checked-out branch is the protected branch or not a work branch | Refuse |
| `-f`, `--force`, `--force-with-lease`, `--force-with-lease=…` or `--force-if-includes` | Refuse |
| A refspec that starts with `+` | Refuse |
| `--delete` or `-d` | Refuse |
| `--all`, `--mirror`, `--tags` or `--follow-tags` | Refuse |
| A word that contains `$` or a backtick | Refuse |
| A refspec equal to the protected branch, or ending in `:main` or `/main` | Refuse |
| No refspec, while the upstream of the current branch is the protected branch | Refuse |
| Anything else | Allow |

The first positional word after `push` is the remote; every positional word after it is a refspec.

## guard-commit.sh

| Condition | Decision |
|---|---|
| `git commit` while the checked-out branch is the protected branch or not a work branch | Refuse |
| A git subcommand produced by an expansion | Refuse |
| Anything else | Allow |

The message is never inspected: only the branch decides.

## guard-merge.sh

For every `gh pr merge` segment:

| Condition | Decision |
|---|---|
| No `--auto` | Refuse: the merge has to wait for the required checks |
| `--admin` | Refuse: it bypasses the required checks |
| An expansion in a word that is not a flag, such as `$PR` | Refuse |
| `gh pr merge --auto …`, and every other `gh` command | Allow |

## Work branches

Commits and pushes are allowed only from a branch that matches one of these patterns, built from `.claude/kit.vars`:

| Lane | Pattern | Example with the template values |
|---|---|---|
| Issue | `^<KIT_ISSUE_BRANCH_PREFIX>[0-9]+-[a-z0-9][a-z0-9._-]*$` | `issue/39-add-the-product-catalog-module` |
| Maintenance | `^<KIT_MAINTENANCE_BRANCH_PREFIX>[a-z0-9][a-z0-9._-]*$` | `kit/fix-push-guard` |

Read-only git commands and branch creation work from any branch, including the protected one, so a session can always leave it to start an issue. They also work from a detached HEAD, where a commit or a push is refused because there is no branch to judge it against.

## session-context.sh

The SessionStart hook restates the repository policy whenever a conversation starts or loses its context:

- the protected branch, and that it changes only through a pull request GitHub merges;
- the two work-branch shapes, and what the guards refuse;
- the current branch and the number of uncommitted entries;
- the issue in progress, read from `.claude/kit-state/current-issue.json`.

The runner writes that file before each session and removes it when the run ends with the pull request merged. A run that ends with the merge only queued leaves it behind, so the hook names the issue as in progress only while that issue's branch is checked out, and sets the session title to `issue-<number>`.

The state file is read with jq and never sourced, because the issue title comes from GitHub. The hook never blocks, falls back to the template values when `kit.vars` is missing, and prints plain text when jq is not installed. Its wording states facts rather than instructions: text framed as out-of-band commands can trip the model's prompt-injection defences.

## Limits

- **Hooks are not a security boundary.** A command the guards do not parse, such as `bash -c "git push origin main"` or a script that runs git, reaches git untouched, and `disableAllHooks` or `--bare` turn the hooks off entirely. The ruleset on GitHub is the barrier; the guards give feedback inside the turn.
- **Quotes are not interpreted.** The tokenizer splits on whitespace, so the words inside a quoted argument are read like any other word. The battery pins the case that matters: a commit message that mentions `main` does not block a valid push.
- **The battery calls each hook directly.** It exercises the parsing and the decisions, not the `if` filter in `settings.json` that decides which commands reach a hook.

## Adding a case

Every change to a hook starts with a case that fails before the change and passes after it.

1. Add a line to the matching section of [`kit/test-guards.sh`](../kit/test-guards.sh): `check <guard>.sh <allow|deny> "<command>"` for a guard, or `check_context <issue|none> "<label>"` for the SessionStart hook.
2. Run `bash kit/install.sh`, then `bash kit/test-guards.sh`, and watch the new case fail.
3. Change the hook under `kit/hooks/`, then run both commands again.
