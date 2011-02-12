# lein-lic Manual
Revision 1.0

You know it's important to release your software under a license of your
choice and your code with a copyright notice and a copying permission
statement. Like your code, however, it's not an "attach and forget"
thing. They need to be maintained properly whenever you update your
software. Yet it's a boring task.

`lein-lic` ('`lic`' for short) is a leiningen plugin to help ease the task
for you.

`lic` can

* place a license file at your project's root directory,
* attach a copyright notice with a copying permission statement to your
  source files, and
* update existing copyright notice.

By default `lic` uses the Eclipse Public License 1.0 and attaches an
auto-generated copyright notice. But you can customize them.

## 1. Getting `lein-lic`

Add this line to your `project.clj`:

> :dev-dependencies [[org.clojars.ksuzuki/lein-lic lic-version]]

where '`lic-version`' is the `lic` version string such as "1.0.0".
Then run '`lein deps`' to get `lic` ready to use from the `lein` command.

## 2. Running `lein-lic`

Run '`lein lic p`' to preview the license and the copyright notice. No
additional changes are required in your `project.clj` if you are fine with
them. When you are ready, just run '`lein lic`'.

Running '`lein lic`' again won't duplicate the license and the copyright
notice. In fact `lic` scans the first couple of lines of source file (100
lines by default), looking for a *valid copyright notice*, and attaches a
copyright notice only when no valid copyright notice is found. In addition,
if a valid copyright notice is found and it's *conforming* to what `lic`
expects but is different from what it should look like, `lic` updates it
accordingly. See "3.2 Valid Copyright Notice" for more details about valid
copyright notice and what exactly 'conforming' means.

If you like, you can let `lic` report processing status of each file using
the '`s`' option. The status lines will look like below.

	A  /Users/ksuzuki/clojure/lic/src/controller.clj  
	   /Users/ksuzuki/clojure/lic/src/core.clj  
	F  /Users/ksuzuki/clojure/lic/src/model.clj  
	U  /Users/ksuzuki/clojure/lic/src/view.clj  

The first letter is a symbol indicating whether a copyright notice is
attached ('`A`') or updated ('`U`'). '`F`' means the file has a conforming 
copyright notice but it's *fixed* so that update action was not taken on the
file (See "3.2 Valid Copyright Notice" for more details about *fixed
conforming copyright notice*). No symbol means the file has a copyright
notice and no update action was taken. An error symbol may be shown when the
file is not writable (`W!`) or something wrong happened (`E!` or `F!`). When
you want to see files with fixed conforming copyright notice only, run `lic`
with the '`f`' option.

Run '`lein lic ?`' to print the `lic` key-value syntax for customization and
the current values.

Running '`lein help lic`' prints the short help message.

## 3. Customizing `lein-lic`

You may want to change some of the values, such as the ones of `:artifact`
and `:author`. To change them, add a `:lic` map to your `project.clj` with
key-values you want to customize, like this:

	:lic {:artifact "your software name - a brief description"  
	      :author   ["Your Name"]}  

The next section explains how to customize the key-values you would use most
often.

### 3.1 The Auto-generated Copyright Notice

`:artifact` and `:author` are the main keys for customizing the
auto-generated copyright notice.

> :artifact "your software name - a brief description"  
> :author   ["Your Name"]  

If you already have `:description` in your `project.clj` and want to use it
as the value for `:artifact`, write `:artifact` like this:

> :artifact :description

You can also add contributor's names to `:author`.

> :author ["Your Name" "Contributor A" "Contributor B"]

If you have been maintaining your software for a couple of years, you may
want to show the years in the copyright notice. Describe the years using
`:year`.

> :year [2000 "2002" "2004, 2005" "2007-2010"]

Run '`lein lic ?`' and print the copyright notice again. Notice that each
line is a Clojure comment line. When you have a Java source file, the
copyright notice will be attached as Java comment instead. You can customize
the comment formats and even provide your own for different file types. See
"4.4 Attaching Copyright Notice to Other File Types".

Notice also that each line is nicely wrapped to fit in the traditional
display column size. If you want to change the wrap position, use `:wrap-at`
with a positive integer.

### 3.2 Valid Copyright Notice

A valid copyright notice ('`vcn`' for short) is a string which matches this
pattern:

> `Copyright (C) years name`

Below are some vcn examples.

> `; Copyright (C) 2011 ksuzuki`

> ` * Copyright (c) 2001, 2005, 2008-2011. ksuzuki`

> `;;;; Copyright (C) 1999, `  
> `;;;; 2008-2010 ksuzuki`  

> ` * blah blah blah blah  Copyright *`  
> ` * (c) 2000, 2005-2011 All rights *`  
>  `* reserved.  *`  

(In the last case 'All' is accidentally identified as name.)

When a vcn is enclosed with `%!` and `!%` tags like this:

> `;; %! Copyright (C) 2008-2011 Kei Suzuki  All rights reserved. !%`

It's called a *conforming copyright notice*. When vcn is conforming, `lic`
can update it. For example, suppose you released the first version of a
program in 2009 and then released the second version in 2011, you may want
to update the year part of the vcn from:

> `;; %! Copyright (C) 2009 Your Name  All rights reserved. !%`

to:

> `;; %! Copyright (C) 2009, 2011 Your Name  All rights reserved. !%`

It's quite easy to do that. In the `lic` map update `:year` value and add:

> :update true

Then run '`lein lic`'.

What if you attached a conforming copyright notice to a file but want to
exclude the file being updated for some reason? Change the first '%!' tag to
something different, preferably '%%'. Such notice is called *fixed conforming
copyright notice*. Files with fixed conforming copyright notice will be
reported with 'F' symbol when `lic` is invoked with the `s` or `f` option. 

## 4. Advanced Usage

### 4.1 Using Different License and File Name

By default `lic` creates a file called `COPYING` at your project's root
directory and writes a copy of the Eclipse Public License 1.0. If you want
to use a different file name, say 'EPL-V10', you can do so using `:file'
like this:

> :file [:default "EPL-V10"]

The first element of the `:file` vector value is either `:default` or a
relative path to a license file you want to use from the project's root
directory, and the second optional element is the file name.

So, for example, when you want to use a GNU Public License in a file called
'`COPYING`', put the file at your project's root directory and wirte `:file`
like this:

    :file ["COPYING" "COPYING"]

Then `lic` can preview it correctly and doesn't replace it with the Eclipse
Public License.

In case you want to release your software under dual licenses you may want
to include those license files with your software and place one of them at
your project's root directory as the default, recommended license. In such
case, you can save those license files in `license/` and write `:file` like
this:

> :file ["license/default.txt"]

### 4.2 Using Different Copyright Notice

You can use a copyright notice you wrote, instead of the auto-generated
one. Save your copyright notice, for example, in `license/notice.txt` and
add `:notice` like this:

> :notice ["license/notice.txt"]

### 4.3 Adding Other Directories to Scan

By default `lic` scans files in `src/` only. When you want `lic` scan files
in other directories in addition, use `:dir` and write the directory paths
relative from your project's root directory, like this:

> :dir ["resources" "extra"]

### 4.4 Attaching Copyright Notice to Other File Types

By default `lic` knows how to attach copyright notice as comment line for
`.clj` and `.java` files only. 

For `.clj` file each line of copyright notice is prefixed with "`;; `". For
`.java` file the start of comment line "`/*`" is written first, then
copyright notice lines prefixed with "` * `", and finally the end of comment
line "` */`" is written.

If you have other file types in `src/` and want to attach copyright notice
to them too, you can let `lic` find those files and attach copyright notice
in proper comment format.

Suppose you want to attach the auto-generated copyright notice to the `.xml`
files in `src/xml/`. Then you write `:notice` like this:

> :notice [:default {:xml ["<\!--" "  -- " "  --\>"]}]

The first element of the `:notice` vector value is either `:default` or a
relative path to a copyright notice file from the project's root directory,
and the second optional element is a map of a file extension keyword (as
key) and a comment format for that file type (as value). A comment format is
either a single string or a vector of up to three strings - the first string
for start of comment line, the second one is a prefix of the second and after
lines, and the last optional one is for end of comment line.

File extension keywords and comment formats for Clojure and Java are
predefined like this:

> {:clj "; ", :java ["/*" " * " " */"]}

And you can override those predefined formats. For example,

> :notice [:default {:clj ";;; ", :java ["/**" " ** " " **/"]}]

Here is another example, combining some of them above.

> :notice ["license/notice.txt" {:clj ";;; ", :xml ["<\!--" "  -- " "  --\>"]}]

This lets `lic` use your copyright notice in `license/notice.txt` and attach
it to `.clj`, `.java`, and `.xml` files, and each line of your copyright
notice in `.clj` file is prefixed with "`;;; `" instead of "`; `".

### 4.5 Scanning Whole Lines Using a Different VCN Pattern

Suppose your code already has a valid copyright notice but it's somewhere
below line 100 and you want `lic` find it to avoid duplicating copyright
notice. Say the copyright notice is on line 321, you can let `lic` scan by
that line using `:scan-line`.

> :scan-line 321

If you want `lic` scan whole lines, write:

> :scan-line 0

You can also change the default vcn pattern using `:vcn-pattern`, if
necessary.

## License

Copyright (c) 2011 Kei Suzuki. All rights reserved.

This document is part of lein-lic - a leiningen license attacher plugin
("This Software").

The use and distribution terms for this software are covered by the Eclipse
Public License 1.0
([http://opensource.org/licenses/eclipse-1.0.php](http://opensource.org/licenses/eclipse-1.0.php))
which can be found in the file COPYING included with this distribution. By
using this software in any fashion, you are agreeing to be bound by the
terms of this license.

You must not remove this notice, or any other, from this software.
