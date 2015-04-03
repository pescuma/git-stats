git-stats
=========

Stats for your git repository


### Running

Add to path and call `git stats` inside a git repository (`git-stats` also work). No command line options are needed. If you want you can pass the path to the repository as an argument and also `-t X` to set the number of threads to run (by default it's number of cores - 1). _It may take a long time to run on big repositories._

It will run `git blame` over all the files in the repository and show some nice grouped information. For example, for this repository it shows:

```
Authors:
   Ricardo Pescuma Domenecci : 100.0% of the lines: 3342 lines (2088 code, 741 comment, 513 empty) in 26 commits from 2013-11 to 2015-04

Months:
   2013-11 : 2403 lines (1332 code, 736 comment, 335 empty) in 5 commits
   2013-12 :  196 lines ( 158 code,   0 comment,  38 empty) in 3 commits
   2014-03 :   18 lines (   9 code,   0 comment,   9 empty) in 4 commits
   2015-03 :   38 lines (  32 code,   2 comment,   4 empty) in 6 commits
   2015-04 :  687 lines ( 557 code,   3 comment, 127 empty) in 8 commits

Languages:
   Java : 2756 lines (1614 code, 729 comment, 413 empty) in 18 commits from 2013-11 to 2015-04
   XML  :  450 lines ( 366 code,  12 comment,  72 empty) in  6 commits from 2013-11 to 2015-04
   Ant  :  105 lines (  86 code,   0 comment,  19 empty) in  3 commits from 2013-11 to 2015-03
   Text :   31 lines (  22 code,   0 comment,   9 empty) in  5 commits from 2013-11 to 2015-03
```

(of course it gets better when you have more people contributing to the repository)
