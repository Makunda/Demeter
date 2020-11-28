package main

import (
	"flag"
	"fmt"
	"github.com/google/logger"
	"os"
)

const logPath = "./logs/info.log"
var verbose = flag.Bool("verbose", false, "Print info level logs to stdout")
var refreshRate = flag.Int("refresh", -1, "Set the refresh rate of the watchdog")


func printGreetings() {
	var logo string = `
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@/              ./@@@@@@                                                                          @@@@@@@@@@@@
@@@@/        ,/           /@@@  d8888b. d88888b .88b  d88. d88888b d888888b d88888b d8888b.             @@@@@@@@@@@@
@@&      //*/////*,///      @@  88   8D 88'     88'YbdP 88 88'      ~~88~~  88'     88   8D             @@@@@@@@@@@@
@/        /**/ / /**/        (  88   88 88ooooo 88  88  88 88ooooo    88    88ooooo 88oobY'             @@@@@@@@@@@@
@      /***//  /*   ////*       88   88 88~~~~~ 88  88  88 88~~~~~    88    88~~~~~ 88 8b               @@@@@@@@@@@@
@       /**,/  /  /****/        88  .8D 88.     88  88  88 88.        88    88.     88  88.             @@@@@@@@@@@@
@              /.               Y8888D' Y88888P YP  YP  YP Y88888P    YP    Y88888P 88   YD             @@@@@@@@@@@@
@/             /             (                                                                          @@@@@@@@@@@@
@@&          #####          @@  Light-weight Go Daemon for Demeter's automation                         @@@@@@@@@@@@
@@@@/     /##########     (@@@	Copyright (C) 2020  Hugo JOBY - Under LGPL v3 License                   @@@@@@@@@@@@
@@@@@@@/,             *(@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@                                                                                                 @@@@@@@@@@@@
@@@@@@@     Options :                                                                                   @@@@@@@@@@@@
@@@@@@@        -verbose : Print info level logs to stdout                                               @@@@@@@@@@@@
@@@@@@@        -refresh int : Set a refresh rate for the daemon                                         @@@@@@@@@@@@
@@@@@@@                                                                                                 @@@@@@@@@@@@
@@@@@@@     Project Github : https://github.com/Makunda/Demeter                                         @@@@@@@@@@@@
@@@@@@@     Extend Url     :                                                                            @@@@@@@@@@@@
@@@@@@@       https://extend.castsoftware.com/#/extension?id=com.castsoftware.uc.demeter&version=2.0.0  @@@@@@@@@@@@
@@@@@@@                                                                                                 @@@@@@@@@@@@
@@@@@@@     Description    :                                                                            @@@@@@@@@@@@
@@@@@@@     Get Information, statistics and solve Use Cases in the twinkling of an eye.                 @@@@@@@@@@@@
@@@@@@@     The Demeter is a project whose ambition is to industrialize the actions on Imaging.         @@@@@@@@@@@@
@@@@@@@     Make it easy to check for application configuration issues, quickly address a list of use   @@@@@@@@@@@@
@@@@@@@     cases and get statistics allowing you to get a deeper and clearer view of your application. @@@@@@@@@@@@
@@@@@@@                                                                                                 @@@@@@@@@@@@
@@@@@@@     This Daemon will watch for changes in the Neo4j Database, to perform actions based on the   @@@@@@@@@@@@
@@@@@@@     tags discovered                                                                             @@@@@@@@@@@@
@@@@@@@                                                                                                 @@@@@@@@@@@@
@@@@@@@     Status :                                                                                    @@@@@@@@@@@@
@@@@@@@     The Daemon is now running in background                                                     @@@@@@@@@@@@
@@@@@@@                                                                                                 @@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
`
	fmt.Println(logo)
}

func main() {
	// Get the configuration
	configuration := GetConfiguration()

	// Set the logger
	flag.Parse()

	lf, err := os.OpenFile(logPath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0660)
	if err != nil {
		fmt.Println("Failed to open log file: %v", err, ". Aborting now...")
		os.Exit(1)
	}

	defer lf.Close()

	// Set verbosity
	defer logger.Init("LoggerExample", *verbose, true, lf).Close()

	// Set Refresh Rate
	if *refreshRate > 1{
		configuration.RefreshRate = *refreshRate
		logger.Info("Refresh rate was changed to '", *refreshRate, "' milliseconds.")
	}

	printGreetings()

	// Load configuration
	GetConfiguration()

	// LaunchWatchDog
	LaunchWatchDog()
}