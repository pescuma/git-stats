git-stats
=========

Stats for your git repository


### Running

Add to path and call `git stats` inside a git repository (`git-stats` also work). No command line options are needed. If you want you can pass the path to the repository as an argument and also `-t X` to set the number of threads to run (by default it's number of cores - 1).

It will run `git blame` over all the files in the repository and show some nice grouped information. For example, for this repository it shows:

```
Authors:
   Ricardo Pescuma Domenecci : 100.0% of the lines: 4039 lines (3455 code, 584 empty) in 13 commits from 2013-11 to 2013-12

Months:
   2013-11 : 2889 lines (2506 code, 383 empty) in 10 commits
   2013-12 : 1150 lines (949 code, 201 empty) in 3 commits

Languages:
   Java : 3330 lines (2847 code, 483 empty) in 11 commits, from 2013-11 to 2013-12 (0 umblamable)
   XML : 382 lines (322 code, 60 empty) in 4 commits, from 2013-11 to 2013-12 (0 umblamable)
   C : 223 lines (201 code, 22 empty) in 1 commits, from 2013-12 to 2013-12 (0 umblamable)
   Ant/XML : 104 lines (85 code, 19 empty) in 2 commits, from 2013-11 to 2013-12 (0 umblamable)
```

(of course it gets better when you have more people contributing to the repository)
