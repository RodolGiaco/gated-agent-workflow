#  Project Context

## Running commands

Permission rules are evaluated per subcommand: in `a && b` or `a | b`, every
part must match a rule on its own. A denial of a compound command usually means
one part is not allowed, not the part you were aiming at. Run one command per
call instead of chaining, and read the file rather than piping it through `tail`.
