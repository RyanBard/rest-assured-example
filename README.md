# rest-assured-example

Start the sample server with:

```
ruby sample-server/ruby/sinatra/messages.rb
```

If you haven't already, you will need to gem install sinatra, json, and
sinatra-contrib:

```
sudo gem install sinatra
sudo gem install sinatra-contrib
sudo gem install json
```

By default, the sample-server is running on port 4567, and I haven't bothered
to put any effort in making it configurable.


To run the restassured integration tests:

```
mvn integration-test
```

These tests are pointing towards the hardcoded ```http://localhost:4567/```.
I haven't bothered making those configurable yet either.  I expect they will
just remain an example for other tests I plan to write.
