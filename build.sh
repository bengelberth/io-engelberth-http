#!/bin/bash

DATE=`date +%Y%m%d-%H%M%S`

# Default options
OPTION_VERBOSE=no

OPTION_ARCHIVE_SOURCE=no
OPTION_ARCHIVE_BIN=no
OPTION_COMPILE_MAIN=yes
OPTION_ECHO_HELP=no
OPTION_COMPILE_FILESERVER=no
OPTION_CLEAN=no
OPTION_BUILD_JSON=no

print_error () {
	echo "\033[0;31merror!\033[0m"
}
print_success () {
	echo "\033[0;32msuccess\033[0m"
}
print_verbose () {
	if [ "$OPTION_VERBOSE" = "yes" ]; then
		echo "$1"
	fi
}
# Archive the source file into a tarball
archive_source () {
	echo -n "Archiving source... "
	OUTPUT_TAR=$(tar -cvzf io.engelberth.http-src.tar.gz src/ 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT_TAR"
		exit $?
	fi
	OUTPUT_CP=$(cp io.engelberth.http-src.tar.gz archive/io.engelberth.http-src-$DATE.tar.gz 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT_CP"
		exit $?
	fi
	print_success
	#if [ "$OPTION_VERBOSE" = "yes" ]; then
	print_verbose "$OUTPUT_TAR"
	print_verbose "$OUTPUT_CP"
	#fi
	
}
archive_bin () {
	echo -n "Archiving binaries... "
	OUTPUT_JAR=$(jar cf io.engelberth.http.jar -C bin . 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT_JAR"
		exit $?
	fi
	OUTPUT_CP=$(cp io.engelberth.http.jar lib/io.engelberth.http-$DATE.jar 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT_CP"
	fi
	print_success
	print_verbose "$OUTPUT_JAR"
	print_verbose "$OUTPUT_CP"
	 
}

compile_main () {
	echo -n "Compiling main source... "
	OUTPUT=$(javac -d bin src/io/engelberth/http/*.java 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT"
		exit $?
	fi
	print_success
	 
	print_verbose "$OUTPUT"
}
compile_fileserver () {
	echo -n "Compiling fileserver... "
	OUTPUT=$(javac -cp bin/:org.json.jar -d bin src/io/engelberth/http/fileserver/*.java 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT"
		exit $?
	fi
	print_success
	print_verbose "$OUTPUT"

}
compile_project () {
	compile_main
	compile_fileserver
}

clean_bin () {
	echo -n "Cleaning binaries... "
	OUTPUT=$(rm -rf bin/* 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT"
		exit $?
	fi
	print_success
	print_verbose "$OUTPUT"
}
build_json () {
	echo "Building JSON jar:"
	echo -n "\tCreating tmp directory... "
	JSON_TMP_DIR=$(mktemp -d 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$JSON_TMP_DIR"
		exit $?
	fi
	print_success
	print_verbose "\tDirectory: $JSON_TMP_DIR"
	
	# Download zip
	echo -n "\tDownloading org.json. source... "
	OUTPUT=$(curl -L -o $JSON_TMP_DIR/json.zip https://github.com/douglascrockford/JSON-java/archive/master.zip 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT"
		exit $?
	fi
	print_success
	print_verbose "$OUTPUT"
	
	#unzip download
	echo -n "\tUnzipping source... "
	OUTPUT=$(unzip -d $JSON_TMP_DIR $JSON_TMP_DIR/json 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT"
		exit $?
	fi
	print_success
	print_verbose "$OUTPUT"
	
	#compile source
	echo -n "\tCompiling source... "
	OUTPUT=$(javac -d $JSON_TMP_DIR $JSON_TMP_DIR/JSON-java-master/*.java 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT"
		exit $?
	fi
	print_success
	print_verbose "$OUTPUT"
	
	#build jar
	echo -n "\tBuilding org.json.jar... "
	OUTPUT=$(jar cf org.json.jar -C $JSON_TMP_DIR org 2>&1)
	if [ $? -ne 0 ]; then
		print_error
		echo "$OUTPUT"
		exit $?
	fi
	print_success
	print_verbose "$OUTPUT"
	
	#success!
}
echo_help () {
	echo "Commands: "
	echo "\t-archive             archive project"
	echo "\t-archive-src         archive source"
	echo "\t-archive-bin         archive binaries"
	echo "\t-verbose             verbose output"
	echo "\t-compile             compile all"
	echo "\t-compile-main        compile main tree only (default)"
	echo "\t-no-compile-main     do not compile main tree"
	echo "\t-compile-fileserver  compile file server only"
	echo "\t-clean               delete compiled binaries"
	echo "\t-build-json          get and build json depend."
	
}
# Sort through arguments

for COMMAND in "$@"
do
	case "$COMMAND" in
		-clean)
			OPTION_CLEAN_BIN=yes
			;;
		-build-json)
			OPTION_BUILD_JSON=yes
			;;
		-archive)
			OPTION_ARCHIVE_SRC=yes
			OPTION_ARCHIVE_BIN=yes
			;;
		-archive-src)
			OPTION_ARCHIVE_SRC=yes
			;;
		-archive-bin)
			OPTION_ARCHIVE_BIN=yes
			;;
		-verbose)
			OPTION_VERBOSE=yes
			;;
		-compile-main)
			OPTION_COMPILE_MAIN=yes
			;;
		-no-compile-main)
			OPTION_COMPILE_MAIN=no
			;;
		-compile)
			OPTION_COMPILE_MAIN=yes
			OPTION_COMPILE_FILESERVER=yes
			;;
		-compile-fileserver)
			OPTION_COMPILE_FILESERVER=yes
			;;
		*)
			echo "Unknown command: $COMMAND" 
			echo_help
			exit 1
			;;
	esac
done
if [ "$OPTION_CLEAN_BIN" = "yes" ]; then
	clean_bin
fi
if [ "$OPTION_COMPILE_MAIN" = "yes" ]; then
	compile_main
fi
if [ "$OPTION_BUILD_JSON" = "yes" ]; then
	build_json
fi
if [ "$OPTION_COMPILE_FILESERVER" = "yes" ]; then
	compile_fileserver
fi
if [ "$OPTION_ARCHIVE_SRC" = "yes" ]; then
	archive_source
fi
if [ "$OPTION_ARCHIVE_BIN" = "yes" ]; then
	archive_bin
fi






