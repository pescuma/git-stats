package org.pescuma.gitstats;

import static org.apache.commons.io.FilenameUtils.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FilenameToLanguage {
	
	// Copied from cloc.pl : Copyright (C) 2006-2013 Northrop Grumman Corporation
	
	private static final Map<String, String> extensions = new HashMap<String, String>();
	static {
		extensions.put("abap", "ABAP");
		extensions.put("ac", "m4");
		extensions.put("ada", "Ada");
		extensions.put("adb", "Ada");
		extensions.put("ads", "Ada");
		extensions.put("adso", "ADSO/IDSM");
		extensions.put("ahk", "AutoHotkey");
		extensions.put("am", "make");
		extensions.put("ample", "AMPLE");
		extensions.put("as", "ActionScript");
		extensions.put("dofile", "AMPLE");
		extensions.put("startup", "AMPLE");
		extensions.put("asa", "ASP");
		extensions.put("asax", "ASP.Net");
		extensions.put("ascx", "ASP.Net");
		extensions.put("asm", "Assembly");
		extensions.put("asmx", "ASP.Net");
		extensions.put("asp", "ASP");
		extensions.put("aspx", "ASP.Net");
		extensions.put("master", "ASP.Net");
		extensions.put("sitemap", "ASP.Net");
		extensions.put("cshtml", "Razor");
		extensions.put("awk", "awk");
		extensions.put("bash", "Bourne Again Shell");
		extensions.put("bas", "Visual Basic");
		extensions.put("bat", "DOS Batch");
		extensions.put("build.xml", "Ant");
		extensions.put("cbl", "COBOL");
		extensions.put("CBL", "COBOL");
		extensions.put("c", "C");
		extensions.put("C", "C++");
		extensions.put("cc", "C++");
		extensions.put("ccs", "CCS");
		extensions.put("cfc", "ColdFusion CFScript");
		extensions.put("cfm", "ColdFusion");
		extensions.put("cl", "Lisp/OpenCL");
		extensions.put("clj", "Clojure");
		extensions.put("cljs", "ClojureScript");
		extensions.put("cls", "Visual Basic");
		extensions.put("cmakelists.txt", "CMake");
		extensions.put("cmake", "CMake");
		extensions.put("cob", "COBOL");
		extensions.put("coffee", "CoffeeScript");
		extensions.put("component", "Visualforce Component");
		extensions.put("config", "ASP.Net");
		extensions.put("cpp", "C++");
		extensions.put("cs", "C#");
		extensions.put("csh", "C Shell");
		extensions.put("css", "CSS");
		extensions.put("ctl", "Visual Basic");
		extensions.put("cxx", "C++");
		extensions.put("d", "D");
		extensions.put("da", "DAL");
		extensions.put("dart", "Dart");
		extensions.put("def", "Teamcenter def");
		extensions.put("dmap", "NASTRAN DMAP");
		extensions.put("dpr", "Pascal");
		extensions.put("dsr", "Visual Basic");
		extensions.put("dtd", "DTD");
		extensions.put("ec", "C");
		extensions.put("el", "Lisp");
		extensions.put("erl", "Erlang");
		extensions.put("exp", "Expect");
		extensions.put("f77", "Fortran 77");
		extensions.put("f90", "Fortran 90");
		extensions.put("f95", "Fortran 95");
		extensions.put("f", "Fortran 77");
		extensions.put("fmt", "Oracle Forms");
		extensions.put("focexec", "Focus");
		extensions.put("frm", "Visual Basic");
		extensions.put("gnumakefile", "make");
		extensions.put("go", "Go");
		extensions.put("groovy", "Groovy");
		extensions.put("gant", "Groovy");
		extensions.put("h", "C/C++ Header");
		extensions.put("hh", "C/C++ Header");
		extensions.put("hpp", "C/C++ Header");
		extensions.put("hrl", "Erlang");
		extensions.put("hs", "Haskell");
		extensions.put("htm", "HTML");
		extensions.put("html", "HTML");
		extensions.put("i3", "Modula3");
		extensions.put("idl", "IDL");
		extensions.put("ism", "InstallShield");
		extensions.put("pro", "IDL");
		extensions.put("ig", "Modula3");
		extensions.put("il", "SKILL");
		extensions.put("ils", "SKILL++");
		extensions.put("inc", "PHP or Pascal");
		extensions.put("ino", "Arduino Sketch");
		extensions.put("pde", "Arduino Sketch");
		extensions.put("itk", "Tcl/Tk");
		extensions.put("java", "Java");
		extensions.put("jcl", "JCL");
		extensions.put("jl", "Lisp");
		extensions.put("js", "Javascript");
		extensions.put("jsf", "JavaServer Faces");
		extensions.put("xhtml", "JavaServer Faces");
		extensions.put("jsp", "JSP");
		extensions.put("ksc", "Kermit");
		extensions.put("ksh", "Korn Shell");
		extensions.put("lhs", "Haskell");
		extensions.put("l", "lex");
		extensions.put("less", "LESS");
		extensions.put("lsp", "Lisp");
		extensions.put("lisp", "Lisp");
		extensions.put("lua", "Lua");
		extensions.put("m3", "Modula3");
		extensions.put("m4", "m4");
		extensions.put("makefile", "make");
		extensions.put("Makefile", "make");
		extensions.put("met", "Teamcenter met");
		extensions.put("mg", "Modula3");
		extensions.put("mli", "ML");
		extensions.put("ml", "ML");
		extensions.put("ml", "OCaml");
		extensions.put("mli", "OCaml");
		extensions.put("mly", "OCaml");
		extensions.put("mll", "OCaml");
		extensions.put("m", "MATLAB/Objective C/MUMPS");
		extensions.put("mm", "Objective C++");
		extensions.put("wdproj", "MSBuild scripts");
		extensions.put("csproj", "MSBuild scripts");
		extensions.put("mps", "MUMPS");
		extensions.put("mth", "Teamcenter mth");
		extensions.put("oscript", "LiveLink OScript");
		extensions.put("pad", "Ada");
		extensions.put("page", "Visualforce Page");
		extensions.put("pas", "Pascal");
		extensions.put("pcc", "C++");
		extensions.put("perl", "Perl");
		extensions.put("pfo", "Fortran 77");
		extensions.put("pgc", "C");
		extensions.put("php3", "PHP");
		extensions.put("php4", "PHP");
		extensions.put("php5", "PHP");
		extensions.put("php", "PHP");
		extensions.put("pig", "Pig Latin");
		extensions.put("plh", "Perl");
		extensions.put("pl", "Perl");
		extensions.put("PL", "Perl");
		extensions.put("plx", "Perl");
		extensions.put("pm", "Perl");
		extensions.put("pom.xml", "Maven");
		extensions.put("pom", "Maven");
		extensions.put("p", "Pascal");
		extensions.put("pp", "Pascal");
		extensions.put("psql", "SQL");
		extensions.put("py", "Python");
		extensions.put("pyx", "Cython");
		extensions.put("qml", "QML");
		extensions.put("rb", "Ruby");
		extensions.put("resx", "ASP.Net");
		extensions.put("rex", "Oracle Reports");
		extensions.put("rexx", "Rexx");
		extensions.put("rhtml", "Ruby HTML");
		extensions.put("rs", "Rust");
		extensions.put("s", "Assembly");
		extensions.put("S", "Assembly");
		extensions.put("scala", "Scala");
		extensions.put("sbl", "Softbridge Basic");
		extensions.put("SBL", "Softbridge Basic");
		extensions.put("sc", "Lisp");
		extensions.put("scm", "Lisp");
		extensions.put("sed", "sed");
		extensions.put("ses", "Patran Command Language");
		extensions.put("pcl", "Patran Command Language");
		extensions.put("ps1", "PowerShell");
		extensions.put("sass", "SASS");
		extensions.put("scss", "SASS");
		extensions.put("sh", "Bourne Shell");
		extensions.put("smarty", "Smarty");
		extensions.put("sql", "SQL");
		extensions.put("sproc.sql", "SQL Stored Procedure");
		extensions.put("spoc.sql", "SQL Stored Procedure");
		extensions.put("spc.sql", "SQL Stored Procedure");
		extensions.put("udf.sql", "SQL Stored Procedure");
		extensions.put("data.sql", "SQL Data");
		extensions.put("v", "Verilog-SystemVerilog");
		extensions.put("sv", "Verilog-SystemVerilog");
		extensions.put("svh", "Verilog-SystemVerilog");
		extensions.put("tcl", "Tcl/Tk");
		extensions.put("tcsh", "C Shell");
		extensions.put("tk", "Tcl/Tk");
		extensions.put("tpl", "Smarty");
		extensions.put("trigger", "Apex Trigger");
		extensions.put("vala", "Vala");
		extensions.put("vapi", "Vala Header");
		extensions.put("vhd", "VHDL");
		extensions.put("vhdl", "VHDL");
		extensions.put("vba", "Visual Basic");
		extensions.put("vbp", "Visual Basic");
		extensions.put("vb", "Visual Basic");
		extensions.put("vbw", "Visual Basic");
		extensions.put("vbs", "Visual Basic");
		extensions.put("webinfo", "ASP.Net");
		extensions.put("xml", "XML");
		extensions.put("mxml", "MXML");
		extensions.put("build", "NAnt scripts");
		extensions.put("vim", "vim script");
		extensions.put("xaml", "XAML");
		extensions.put("xsd", "XSD");
		extensions.put("xslt", "XSLT");
		extensions.put("xsl", "XSLT");
		extensions.put("y", "yacc");
		extensions.put("yaml", "YAML");
		extensions.put("yml", "YAML");
	};
	
	private static final Map<String, String> filenames = new HashMap<String, String>();
	static {
		filenames.put("makefile", "make");
		filenames.put("gnumakefile", "make");
		filenames.put("cmakelists.txt", "CMake");
		filenames.put("build.xml", "Ant/XML");
		filenames.put("pom.xml", "Maven/XML");
	};
	
	public static String detectLanguage(String filename) {
		if (filename == null)
			return "";
		
		String result = filenames.get(getName(filename).toLowerCase(Locale.ENGLISH));
		if (result != null)
			return result;
		
		result = extensions.get(getDoubleExtension(filename).toLowerCase(Locale.ENGLISH));
		if (result != null)
			return result;
		
		result = extensions.get(getExtension(filename).toLowerCase(Locale.ENGLISH));
		if (result != null)
			return result;
		
		return "";
	}
	
	private static String getDoubleExtension(String filename) {
		int pos = filename.lastIndexOf('.');
		if (pos < 0)
			return "";
		
		pos = filename.lastIndexOf('.', pos);
		if (pos < 0)
			return "";
		
		return filename.substring(pos + 1);
	}
	
	public static boolean isKnownFileType(String filename) {
		return !detectLanguage(filename).isEmpty();
	}
}
