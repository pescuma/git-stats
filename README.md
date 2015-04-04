git-stats
=========

Stats for your git repository


### Running

Add to path and call `git stats` inside a git repository (`git-stats` also work). No command line options are needed. If you want you can pass the path to the repository as an argument and also `-t X` to set the number of threads to run (by default it's number of cores - 1). _It may take a long time to run on big repositories._

It will run `git blame` over all the files in the repository and show some nice grouped information. For example, for this repository it shows:

```
Authors:
   Ricardo Pescuma Domenecci : 100.0% of the lines: 2938 lines (1758 code, 728 comment, 452 empty) in 29 commits from 2013-11 to 2015-04

Months:
   2013-11 : 2106 lines (1100 code, 727 comment, 279 empty) in  5 commits
   2013-12 :  184 lines ( 149 code,   0 comment,  35 empty) in  3 commits
   2014-03 :   18 lines (   9 code,   0 comment,   9 empty) in  4 commits
   2015-03 :   20 lines (  18 code,   0 comment,   2 empty) in  5 commits
   2015-04 :  610 lines ( 482 code,   1 comment, 127 empty) in 12 commits

Languages:
   Java : 2783 lines (1634 code, 728 comment, 421 empty) in 19 commits from 2013-11 to 2015-04
   Ant  :   90 lines (  75 code,   0 comment,  15 empty) in  5 commits from 2013-11 to 2015-04
   XML  :   34 lines (  27 code,   0 comment,   7 empty) in  8 commits from 2013-11 to 2015-04
   Text :   31 lines (  22 code,   0 comment,   9 empty) in  5 commits from 2013-11 to 2015-04
```

(of course it gets better when you have more people contributing to the repository)
