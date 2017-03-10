#PaystackTools

RESTful microservice, powered by Spring Boot, one-button bootstrap

##To run the program:

- Clone this repository

- `./gradlew bootRun`

##Available options:

`-Dserver.port` - port for this application

`-DadminPaymentsFilePath` - path to admin file with input data

`-DpaystackPaymentsFilePath` - path to paystack file with input data

`-DfraudLevel` - negative balance level to consider user fraudulent

##RESTful API:

Thanks to Spring HATEOAS, the service provides easy access to the state:

**NB!** JSONView plugin for chrome is highly recommended: https://chrome.google.com/webstore/detail/jsonview/chklaanhfefbnpoihckbnefhakgolnmc/related?hl=en

User API will be available at `http://localhost:8080/useers` + `{?page,size,sort}`

Movie search API reference will be available at `http://localhost:8080/users/search`

##Quick tutorial:

- Git clone or download this, unpack

- Go to the root of the project in terminal and run `./gradlew bootRun -DadminPaymentsFilePath=<path-to-admin-file> -DpaystackPaymentsFilePath=<path-to-paystack-file> -DfraudLevel=3000`

- Open `http://localhost:8080/users/search/findByResolution?resolution=overcharge` to see the data
