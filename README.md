git-stats
=========

Stats for your git repository


### Running

Add to path and call `git stats` inside a git repository (`git-stats` also work). No command line options are needed. If you want you can pass the path to the repository as an argument and also `-t X` to set the number of threads to run (by default it's number of cores - 1). _It may take a long time to run on big repositories._

It will run `git blame` over all the files in the repository and show some nice grouped information. For example, for this repository it shows:

```
Authors:
   Ricardo Pescuma Domenecci : 100.0% of the lines: 5039 lines (4268 code, 771 empty) in 20 commits from 2013-11 to 2014-02

Months:
   2013-11 : 2893 lines (2509 code, 384 empty) in 11 commits
   2013-12 : 2113 lines (1736 code, 377 empty) in 3 commits
   2014-01 : 2 lines (1 code, 1 empty) in 1 commits
   2014-02 : 31 lines (22 code, 9 empty) in 5 commits

Languages:
   Java : 3335 lines (2851 code, 484 empty) in 13 commits, from 2013-11 to 2014-02
   Text : 995 lines (809 code, 186 empty) in 6 commits, from 2013-11 to 2014-02
   XML : 382 lines (322 code, 60 empty) in 4 commits, from 2013-11 to 2013-12
   C : 223 lines (201 code, 22 empty) in 1 commits, from 2013-12 to 2013-12
   Ant/XML : 104 lines (85 code, 19 empty) in 2 commits, from 2013-11 to 2013-12
```

(of course it gets better when you have more people contributing to the repository)
