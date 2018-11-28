#!/bin/bash

jxm_mem="get -d java.lang -b java.lang:type=Memory *"
jmx_log_output_dit="jmx_dir/"
java_cmd="java"
case $1 in
	g1)
		echo "Start with G1"
		flags="-XX:+UseG1GC"
		jmx_old="get -d java.lang -b java.lang:type=GarbageCollector,name=G1\ Old\ Generation *"
		jmx_young="get -d java.lang -b java.lang:type=GarbageCollector,name=G1\ Young\ Generation *"
		;;
	serial_serial)
		echo "Start with serial (DefNew) for YoungGen and serial mark sweep compact for oldGen"
		flags="-XX:+UseSerialGC"
		jmx_old="get -d java.lang -b java.lang:type=GarbageCollector,name=Copy *"
		jmx_young="get -d java.lang -b java.lang:type=GarbageCollector,name=MarkSweepCompact *"
		;;
	ps_ps)
		echo "Start with parallel scavenge (PSYoungGen) for YoungGen and serial mark sweep compact (PSOldGen) for OldGen"
		flags="-XX:+UseParallelGC"
		jmx_old="get -d java.lang -b java.lang:type=GarbageCollector,name=PS\ Scavenge *"
		jmx_young="get -d java.lang -b java.lang:type=GarbageCollector,name=PS\ MarkSweep *"
		;;
	par_serial)
		echo "Start with parallel (ParNew) for YoungGen and serial mark sweep for OldGen"
		flags="-XX:+UseParNewGC"
		jmx_old="get -d java.lang -b java.lang:type=GarbageCollector,name=ParNew *"
		jmx_young="get -d java.lang -b java.lang:type=GarbageCollector,name=MarkSweepCompact *"
		;;
	par_cms)
		echo "Start with Parallel (ParNew) for YoungGen and CMS for OldGen"
		flags="-XX:+UseParNewGC -XX:+UseConcMarkSweepGC"
		jmx_old="get -d java.lang -b java.lang:type=GarbageCollector,name=Copy *"
		jmx_young="get -d java.lang -b java.lang:type=GarbageCollector,name=ConcurrentMarkSweep *"
		;;
	shenandoah)
		java_cmd="/Users/sergey/shenandoah/shenandoah/build/macosx-x86_64-normal-server-release/images/jdk/bin/java"
		echo "Start with Shenandoah"
		flags="-XX:+UseShenandoahGC"
		jmx_old="get -d java.lang -b java.lang:type=GarbageCollector,name=ShenandoahGC *"
		jmx_young="get -d java.lang -b java.lang:type=GarbageCollector,name=ConcurrentMarkSweep *"
		;;
	*)
		echo "Provide gc type: g1, serial_serial, ps_ps, par_serial, par_cms"
		exit 1
		;;
esac

$java_cmd $flags -jar target/money-transfer-1.0.0-SNAPSHOT-shaded.jar & #1>/dev/null 2>&1 &
pid=$!
sleep 2
python3 run_tests.py > $jmx_log_output_dit/$1_stat &
test_pid=$!
sleep 40

printf "open $pid\n${jmx_young}\n${jmx_old}\n${jxm_mem}\n" | java -jar /Users/sergey/Downloads/jmxterm.jar -n -o $jmx_log_output_dit/$1
kill -15 $test_pid
sleep 2
kill -15 $pid

