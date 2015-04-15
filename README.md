git-stats
=========

Stats for your git repository


### Running

Add to path and call `git stats` inside a git repository (`git-stats` also work). No command line options are needed. If you want you can pass the path to the repository as an argument and also `-t X` to set the number of threads to run (by default it's number of cores - 1). _It may take a long time to run on big repositories._

It will run `git blame` over all the files in the repository and show some nice grouped information. For example, for this repository it shows:

```
Total:
   1995 lines (1611 code, 9 comment, 375 empty) in 13 files in 5 languages in 43 commits from 2013-11 to 2015-04

Authors:
   Ricardo Pescuma Domenecci : 100.0% of the lines: 1995 lines (1611 code, 9 comment, 375 empty) in 13 files in 5 languages in 43 commits from 2013-11 to 2015-04

Months: ▂▂▁▁█
   2013-11 :  170 lines ( 126 code, 2 comment,  42 empty) in  6 files in 4 languages in  4 commits by 1 authors
   2013-12 :  174 lines ( 139 code, 0 comment,  35 empty) in  4 files in 3 languages in  3 commits by 1 authors
   2014-03 :   18 lines (   9 code, 0 comment,   9 empty) in  2 files in 2 languages in  4 commits by 1 authors
   2015-03 :   18 lines (  16 code, 0 comment,   2 empty) in  3 files in 3 languages in  5 commits by 1 authors
   2015-04 : 1615 lines (1321 code, 7 comment, 287 empty) in 11 files in 5 languages in 27 commits by 1 authors

Languages:
   Java : 1094 lines (858 code, 4 comment, 232 empty) in 9 files in 29 commits by 1 authors from 2013-11 to 2015-04
   HTML :  723 lines (613 code, 5 comment, 105 empty) in 1 files in  2 commits by 1 authors from 2015-04 to 2015-04
   Ant  :   89 lines ( 74 code, 0 comment,  15 empty) in 1 files in  5 commits by 1 authors from 2013-11 to 2015-04
   Text :   55 lines ( 39 code, 0 comment,  16 empty) in 1 files in  7 commits by 1 authors from 2013-11 to 2015-04
   XML  :   34 lines ( 27 code, 0 comment,   7 empty) in 1 files in  9 commits by 1 authors from 2013-11 to 2015-04```

(of course it gets better when you have more people contributing to the repository)


### Multiple repositories

You can use:
```
git stats <path to repo 1> <path to repo 2>
```

If it takes too long you can process it one at a time:
```
git stats <path to repo 1> -o 1.csv
git stats <path to repo 2> -o 2.csv
git stats 1.csv 2.csv
```

### Options

Also, you can fix some common problems:
   - Ignore some revisions: `git stats 1.csv 2.csv -i 98ad99e -i 123456`
   - Rename/group authors: `git stats 1.csv 2.csv -a "jh=John" -a "abc=My Pretty Name"`
   - Exclude path: `git stats 1.csv 2.csv -ep a/b`

You can also export to CSV and HTML: `git stats -o a.csv -o a.html -o -`
