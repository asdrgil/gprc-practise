#!/ bin/sh

SERVICE_NAME=coffee
HOME_PATH=/home/user-iot/gprc-practise

PATH_TO_JAR=/home/user-iot/gprc-practise/target/coffeeservice-1.0.0-SNAPSHOT-jar-with-dependencies.jar
PID_PATH_NAME=/tmp/MyService-pid



#  nohup java -jar /home/user-iot/gprc-practise/target/coffeeservice-1.0.0-SNAPSHOT-jar-with-dependencies.jar /tmp 2>> /dev/null >> /dev/null &

case $1 in
 #    start)
 #        echo "1 Starting $SERVICE_NAME ..."
 #            nohup java -jar $PATH_TO_JAR 2>> /dev/null >> /dev/null &
 #                        echo $! > $PID_PATH_NAME
 #            echo "2 $SERVICE_NAME started ..."
 #    ;;
 #    stop)
 #            PID=$(cat $PID_PATH_NAME);
 #            echo "0 STOP $PID PID .............."
 #            echo "4 $SERVICE_NAME stoping ..."
 #            kill $PID;
 #            echo "5 $SERVICE_NAME stopped ..."
 #            rm $PID_PATH_NAME
	# ;;

    start)
        echo "1 Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -jar $PATH_TO_JAR 2>> /dev/null >> /dev/null &
                        echo $! > $PID_PATH_NAME
            echo "2 $SERVICE_NAME started ..."
        else
            echo "3 $SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "0 STOP $PID PID .............."
            echo "4 $SERVICE_NAME stoping ..."
            kill $PID;
            echo "5 $SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "6 $SERVICE_NAME is not running ..."
        fi
    ;;


    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "0 RESTART $PID PID .............."
            echo "7 $SERVICE_NAME stopping ...";
            kill $PID;
            echo "8 $SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "9 $SERVICE_NAME starting ..."
            nohup java -jar $PATH_TO_JAR 2>> /dev/null >> /dev/null &
                        echo $! > $PID_PATH_NAME
            echo "10 $SERVICE_NAME started ..."
        else
            echo "11 $SERVICE_NAME is not running ..."
        fi
    ;;
esac

