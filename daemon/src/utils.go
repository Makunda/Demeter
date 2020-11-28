package main

func ContainsStringList(list []string, x string) bool {
	for _, item := range list {
		if item == x {
			return true
		}
	}
	return false
}