package main

import (
	"github.com/google/logger"
	"github.com/neo4j/neo4j-go-driver/neo4j"
	"time"
)

var warnList map[string]int
var blackList []string

func addWarningApplication(appName string) {

	logger.Error("An error occurred for application with name : ", appName)

	if warnNum, ok := warnList[appName]; ok {
		if warnNum > 5 {
			blackList = append(blackList, appName)
			logger.Error("Application ", appName, " was blacklisted due to too many warn.")
		} else {
			warnList[appName] = warnNum + 1
			logger.Error("Warn : ", warnNum+1, "/", 5)
		}
	} else {
		warnList[appName] = 1
		logger.Error("Warn : ", 1, "/", 5)
	}
}

func watchDemeterTags() {
	// Retrieve actual session
	var session neo4j.Session
	session, _ = GetSession()

	result , err := session.Run("MATCH (o:Object) WHERE EXISTS(o.Tags) AND any( x IN o.Tags WHERE x CONTAINS $prefix ) RETURN DISTINCT [ x IN LABELS(o) WHERE NOT x='Object'] as application, COUNT(o) as numTags;", map[string]interface{}{"prefix": "Dmg_"})

	if err != nil {
		logger.Error("Failed to check if tags are present in the Database.")
	}

	var appList []string

	// Parse the result
	for result.Next() {
		record := result.Record()
		applications, _ := record.Get("application")
		numTags, _ := record.Get("numTags")
		logger.Info(numTags, " tags were found in application ", applications)

		appArray := applications.([]interface{})

		// Verify is the app is not detected yet, and add it to the list
		for _, a := range appArray {
			appName := a.(string)
			if !ContainsStringList(appList, appName) {
				appList = append(appList, appName)
			}
		}
	}

	// If Tags are detected, launch the Demeter Grouping  for each  application
	for _, app := range appList {

		// Ignore the application if blacklisted
		if ContainsStringList(blackList, app) {
			continue
		}

		_ , err := session.Run("CALL demeter.groupTags($application);", map[string]interface{}{"application": app})
		if err != nil {
			// If produce an error, add a warning. If too many warning, the application will be ignored
			addWarningApplication(app)
		}
	}
}


func LaunchWatchDog() {

	configuration := GetConfiguration()
	refresh := configuration.RefreshRate

	for true {
		watchDemeterTags()

		// Wait
		time.Sleep(time.Duration(refresh) * time.Millisecond)
	}
}