for filename in `ls -A target/n-cube*.jar`
  do
    java -jar -Dspring.profiles.active=$1 $filename
  done
