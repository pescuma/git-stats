git-stats
=========

Stats for your git repository


### Running

Add to path and call `git stats` inside a git repository (`git-stats` also work). No command line options are needed. If you want you can pass the path to the repository as an argument and also `-t X` to set the number of threads to run (by default it's number of cores - 1). _It may take a long time to run on big repositories._

It will run `git blame` over all the files in the repository and show some nice grouped information. For example, for this repository it shows:

```
Authors:
   Ricardo Pescuma Domenecci : 100.0% of the lines: 5464 lines (3777 code, 835 comment, 852 empty) in 26 commits from 2013-11 to 2015-03

Months:
   2013-11 : 2836 lines (1723 code, 739 comment, 374 empty) in 10 commits
   2013-12 : 2093 lines (1686 code, 32 comment, 375 empty) in 3 commits
   2014-03 : 33 lines (24 code, 0 comment, 9 empty) in 6 commits
   2015-03 : 502 lines (344 code, 64 comment, 94 empty) in 7 commits

Languages:
   Java : 3757 lines (2395 code, 798 comment, 564 empty) in 19 commits, from 2013-11 to 2015-03
   Text : 996 lines (811 code, 0 comment, 185 empty) in 6 commits, from 2013-11 to 2014-03
   XML : 384 lines (311 code, 11 comment, 62 empty) in 4 commits, from 2013-11 to 2015-03
   C : 223 lines (175 code, 26 comment, 22 empty) in 1 commits, from 2013-12 to 2013-12
   Ant : 104 lines (85 code, 0 comment, 19 empty) in 2 commits, from 2013-11 to 2013-12
```

(of course it gets better when you have more people contributing to the repository)
