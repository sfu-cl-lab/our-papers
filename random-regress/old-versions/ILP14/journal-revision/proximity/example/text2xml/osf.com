people.com
	objectType	VARCHAR
	professional_name	VARCHAR
	birth_name	VARCHAR
	gender		VARCHAR
	date_of_birth	DATE
	date_of_death	DATE
	numberOscars	INTEGER

movies.com
	objectType	VARCHAR
	title		VARCHAR
	genre		VARCHAR
	release_year	INTEGER
	numberOscars	INTEGER
