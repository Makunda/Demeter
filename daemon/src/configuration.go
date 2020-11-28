package main

import (
    "encoding/json"
    "os"
    "fmt"
    "github.com/google/logger"
)



type Neo4jConfiguration struct {
    Url			string 	`json:"url"`
    Username 	string  `json:"username"`
    Password	string 	`json:"password"`
    Encrypted	bool  	`json:"encrypted"`
}

type Configuration struct {
    Neo4j    	Neo4jConfiguration 	`json:"neo4j"`
    RefreshRate int 				`json:"refreshRate"`	
}

var configuration Configuration

func loadConfiguration() {
	jsonFile, err := os.Open("conf.json")
	if err != nil {
		logger.Fatalf("Failed to open Configuration file : %v", err, ". Aborting now...")
		os.Exit(2)
	}

	defer jsonFile.Close()

	decoder := json.NewDecoder(jsonFile)
	configuration = Configuration{}

	err = decoder.Decode(&configuration)
	if err != nil {
		fmt.Println("The configuration is not in a good JSON format :", err)
		os.Exit(3)
	}

	logger.Info("Loaded configuration : ", configuration)
}

func GetConfiguration() *Configuration {
	if (Configuration{}) == configuration {
		loadConfiguration()
	}

	return &configuration
}

