# include both .scala AND .java files
DIR=$1
JARNAME=$2
BUILDDIR="classes"

BEGINPATH=$pwd
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"

JARS="geometry.jar jackson-annotations-2.9.5.jar jackson-core-2.9.5.jar jackson-databind-2.9.5.jar jackson-module-scala_2.12-2.9.5.jar"
JARPREFIX="$SCRIPTPATH/jars/"

LIBJAR=""

for jar in $JARS
do
  LIBJAR="$LIBJAR$JARPREFIX$jar:"
done

LIBJAR=${LIBJAR::-1}


cd $SCRIPTPATH/$DIR

mkdir $BUILDDIR

RET=0

if [[ -d "java" && -d "scala" ]]; then
    echo "oba"
    scalac -deprecation -cp $LIBJAR scala/*.scala java/*.java -d $BUILDDIR
    
    javac -d $BUILDDIR -classpath $LIBJAR:$SCALA_HOME/lib/scala-library.jar:$BUILDDIR java/*.java
else
    if [ -d "java" ]; then
    echo "java"
        javac -d $BUILDDIR -classpath $LIBJAR:$SCALA_HOME/lib/scala-library.jar java/*.java
    else
    echo "scala"
        scalac -deprecation -cp $LIBJAR scala/*.scala -d $BUILDDIR
    fi
fi
       
echo $RET

if [ -d "$BUILDDIR" ]; then
    cd $BUILDDIR
    jar cvf $JARNAME.jar *
    mv $JARNAME.jar ../
fi

cd $BEGINPATH

if [ -d "$SCRIPTPATH/$DIR/$BUILDDIR" ]; then
    rm -r $SCRIPTPATH/$DIR/$BUILDDIR
fi

if [ ! -f $SCRIPTPATH/$DIR/$JARNAME.jar ]; then
  exit 1
fi
