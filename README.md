#PaystackTools

RESTful microservice, powered by Spring Boot, one-button bootstrap

##To run the program:

- Clone this repository

- `./gradlew bootRun`

##Available options:

`-Dserver.port` - port for this application

`-DsrcPaymentsFilePath` - path to src file with input data

`-DfullView` - toggle full or compact view of user payments

`-DpaystackPaymentsFilePath` - path to paystack file with input data

`-DfraudLevel` - negative balance level to consider user fraudulent

##RESTful API:

Thanks to Spring HATEOAS, the service provides easy access to the state:

**NB!** JSONView plugin for chrome is highly recommended: 
https://chrome.google.com/webstore/detail/jsonview/chklaanhfefbnpoihckbnefhakgolnmc/related?hl=en

User API will be available at `http://localhost:8080/users` + `{?page,size,sort}`

Movie search API reference will be available at `http://localhost:8080/users/search`

##Quick tutorial:

- Git clone or download this, unpack

###Launch

### 1)

#### A

- Go to the root of the project and copy secret files `payment_src.csv` and `payment_paystack_test.csv` 
into `src/main/resources` folder, names of the files should be exactly like this otherwise use path `B`

- Go to the root of the project in terminal and run 

`./gradlew bootRun`

#### B

- Go to the root of the project in terminal and run 

`./gradlew bootRun -DsrcPaymentsFilePath=<path-to-src-file> -DpaystackPaymentsFilePath=<path-to-paystack-file> -DfraudLevel=3000 -DfullView=false`

###2)

- Open `http://localhost:8080/run` in your browser

### Navigate

- Open `http://localhost:8080/users/search/findByResolution?resolution=overcharge` to see users with double payments

- Open `http://localhost:8080/users/search/findByResolution?resolution=fraud` to see fraud users 
(balance < -3000 by default, use `-DfraudLevel=<your-number>` to tune this)

- Open `http://localhost:8080/users/search/findByResolution?resolution=undercharge` to see users with negative balance
but lower than fraud level

- Open `http://localhost:8080/users/search/findByResolution?resolution=ok` to see users with no problems

- Open `http://localhost:8080/users/search/findByBalanceLessThan?balance=-10000` 
to see users with balance less than -10000 Naira (or your number)

