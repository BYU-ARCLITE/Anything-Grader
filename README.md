Anything Grader
===============
Written by Joshua Monson for the ARCLITE Lab, Brigham Young University. Last updated January 2013.
This project is open source and designed to assist with the creation of online learning systems.

Requirements
------------
 * Play Framework 2.0.4
 * MySQL

Installation instructions
-------------------------
I'm going to assume that you have the Play Framework already installed and know, more or less, how to run a Play application. If you don't go to http://www.playframework.org/ and learn how.

### 1. Download
Download the code. Awesome.

### 2. Create the database
Add a MySQL database named <code>anything_grader</code> or whataver you want to call it. Just remember what you do call it because you'll need to update the configuration file with the database name.

### 3. Configure the application
Edit the conf/application.conf file and update it to match your MySQL database. The default configuration is assuming that the database name is <code>anything_grader</code>, the user is <code>root</code>, and the password is also <code>root</code>.

    db.default.url="jdbc:mysql://localhost/anything_grader?characterEncoding=UTF-8"
    db.default.user=root
    db.default.password=root

### 4. Run the application
From the console, run the application. There are different ways of doing this, the simplest being:

    play start -DapplyEvolutions.default=true
    
The application will be running at <code>http://localhost:9000/</code>
