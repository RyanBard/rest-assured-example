require 'sinatra'
require 'sinatra/json'
require 'sinatra/cookies'
require 'json'

# sudo gem install sinatra
# sudo gem install sinatra-contrib
# sudo gem install json


###############################################
# begin Session service code
###############################################
@@userIdSequence = 301
@@users = Hash.new

# starting off with a default admin and guest user/password
@@users["admin"] = {username: "admin", password: "password123", isAdmin?: true, name: "Administrator"}
@@users["guest"] = {username: "guest", password: "password123", isAdmin?: false, name: "Guest"}

def checkCredentials(username, password)
    user = @@users[username]
    user and user[:password] == password
end

def getUser(username)
    @@users[username]
end
###############################################
# end Session service code
###############################################


###############################################
# begin Session service code
###############################################
@@sessionIdSequence = 200
@@sessions = Hash.new

def createSession(data)
    sessionId = @@sessionIdSequence.to_s
    @@sessionIdSequence += 1
    @@sessions[sessionId] = data
    sessionId
end

def getSessions()
    @@sessions
end

def getSession(sessionId)
    @@sessions[sessionId]
end

def removeSession(sessionId)
    @@sessions.delete sessionId
end
###############################################
# end Session service code
###############################################


###############################################
# begin Messages service code
###############################################
@@messageIdSequence = 100
@@messages = Hash.new

def getMessage(messageId)
    @@messages[messageId]
end

def removeMessage(messageId)
    @@messages.delete messageId
end

def getAllMessages()
    @@messages.values
end

def saveMessage(message)
    @@messages[message["id"]] = message
end

def createOrUpdate(message)
    unless message["id"]
        message["createdOn"] = Time.now
        message["id"] = @@messageIdSequence
        @@messageIdSequence += 1
    end
    message["lastUpdatedOn"] = Time.now
    saveMessage message
    message
end
###############################################
# end Messages service code
###############################################


def getRequestData()
    jsonBody = request.body.read
    JSON.parse jsonBody
end

post '/messages' do
    halt 401 unless isLoggedIn()
    message = getRequestData()
    json createOrUpdate(message)
end

def postOrPut(url, &block)
    post(url, &block)
    put(url, &block)
end

postOrPut '/messages/:messageId' do
    halt 401 unless isLoggedIn()
    messageId = params[:messageId].to_i
    message = getRequestData()
    halt 400, 'cannot change message id' unless message["id"] == messageId
    json createOrUpdate(message)
end

get '/messages' do
    halt 401 unless isLoggedIn()
    json getAllMessages()
end

get '/messages/:messageId' do
    halt 401 unless isLoggedIn()
    messageId = params[:messageId].to_i
    message = getMessage(messageId)
    halt 404 unless message
    json message
end

delete '/messages/:messageId' do
    halt 401 unless isLoggedIn()
    messageId = params[:messageId].to_i
    message = removeMessage(messageId)
    halt 404 unless message
    json message
end

def getLoggedInSession()
    sessionId = cookies[:sessionId]
    getSession(sessionId)
end

def getLoggedInUser()
    getLoggedInSession()[:user]
end

def isLoggedIn()
    getLoggedInSession()
end

def isAdmin()
    getLoggedInUser()[:isAdmin?]
end

get '/' do
    return redirect '/home' if isLoggedIn()
    %{
<html>
    <head>
        <title>Login</title>
    </head>
    <body>
        <form action="/login" method="post">
            <p>Default user/pass is admin/password123 and guest/password123</p>
            <p>Username: <input type="text" name="username" value="admin"></p>
            <p>Password: <input type="password" name="password" value="password123"></p>
            <input type="submit" value="Login">
        </form>
    </body>
</html>
    }
end

get '/home' do
    return redirect '/' unless isLoggedIn()
    loggedInUserName = getLoggedInUser()[:name]
    template = %{
<html>
    <head>
        <title>Home</title>
    </head>
    <body>
        <p>This is the home page. Hello <%= loggedInUserName %>!</p>
        <% if isAdmin %>
        <p><a href="/loggedInUsers">Logged In Users</a></p>
        <% end %>
        <form action="/logout" method="post">
            <input type="submit" value="Logout">
        </form>
    </body>
</html>
    }
    erb template, {locals: {loggedInUserName: loggedInUserName, isAdmin: isAdmin()}}
end

get '/loggedInUsers' do
    return redirect '/' unless isLoggedIn()
    halt 401, "Must be an admin" unless isAdmin()
    template = %{
<html>
    <head>
        <title>Logged In Users</title>
    </head>
    <body>
        <p>This is the currently logged in users:</p>
        <ol>
        <% loggedInUserSessions.each do |sessionId, userSession| %>
            <li><%= userSession[:user][:name] %> - <%= sessionId %> - <%= userSession[:loginTime] %></li>
        <% end %>
        </ol>
        <p><a href="/home">Home</a></p>
    </body>
</html>
    }
    puts "getSessions is: #{getSessions()}"
    erb template, {locals: {loggedInUserSessions: getSessions()}}
end

post '/login' do
    username = params["username"]
    password = params["password"]
    credentialsMatched = checkCredentials(username, password)
    halt 401, "Invalid credentials" unless credentialsMatched
    sessionId = createSession({loginTime: Time.now, user: getUser(username)})
    cookies[:sessionId] = sessionId
    redirect '/home'
end

post '/logout' do
    removeSession(cookies[:sessionId])
    redirect '/'
end
