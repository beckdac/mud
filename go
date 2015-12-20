#!/bin/bash

CP=".:/home/dacb/.m2/repository/org/mongodb/morphia/morphia/1.0.1/morphia-1.0.1.jar:/home/dacb/.m2/repository/org/mongodb/mongo-java-driver/3.0.4/mongo-java-driver-3.0.4.jar:target/classes" 

echo building
javac -cp "$CP" Test.java MudPlayer.java MudItem.java MudExit.java MudRoom.java MudItemMapHelper.java

mkdir -p mud
cd mud
ln -sf ../*.class .
cd ..

echo running
java -cp "$CP" mud.Test

mongo << EOF
use mud
db.rooms.find()
//db.rooms.drop()
db.players.find()
//db.players.drop()
EOF
