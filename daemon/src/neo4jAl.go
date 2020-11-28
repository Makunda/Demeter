package main

import (
	"github.com/neo4j/neo4j-go-driver/neo4j"
)

var session neo4j.Session = nil

func Connect() (neo4j.Driver, error) {

	var configuration *Configuration = GetConfiguration()

	driver, err := neo4j.NewDriver(configuration.Neo4j.Url, neo4j.BasicAuth(configuration.Neo4j.Username, configuration.Neo4j.Password, ""), func(c *neo4j.Config) {
		c.Encrypted = configuration.Neo4j.Encrypted
	})
	if err != nil {
		return nil, err
	}

	return driver, nil
}

func GetSession() (neo4j.Session, error) {
	if nil == session {

		driver, err := Connect()
		if err != nil {
			return nil, err
		}

		session, err = driver.Session(neo4j.AccessModeWrite)
		if err != nil {
			return nil, err
		}
		defer session.Close()

	}

	return session, nil
}