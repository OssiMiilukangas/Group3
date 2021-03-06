const express = require("express");
const bodyParser = require("body-parser");
const bcrypt = require("bcryptjs");
const passport = require("passport");
const jwt = require("jsonwebtoken");
const jwtStrategy = require("passport-jwt").Strategy,
  ExtractJwt = require("passport-jwt").ExtractJwt;
const jwtSecretKey = require("./jwt-key.json");
const multer = require("multer");
const multerUpload = multer({ dest: "images/" });
const dbconf = require("./db_cfg.json");
const e = require("express");
const mysql = require("mysql");
const app = express();
const port = 3000;

app.use(bodyParser.json({limit: '50mb'}));

// Create MySQL connection
var db = mysql.createPool({
  connectionLimit: 10,
  host: dbconf.host,
  user: dbconf.user,
  password: dbconf.password,
  database: dbconf.database,
  timezone: 'UTC'
});
  
// JWT authentication strategy
let options = {};

options.jwtFromRequest = ExtractJwt.fromAuthHeaderAsBearerToken();
options.secretOrKey = jwtSecretKey.secret;

passport.use(
  new jwtStrategy(options, function (jwt_payload, done) {
    // test if the token is expired
    const now = Date.now() / 1000;

    // for testing tokens don't expire
    if (true) {
      done(null, jwt_payload.user);
    } else {
      done(null, false);
    }
  })
);

/*********************************************
 * USER ENDPOINTS
 ********************************************/

app.post("/user/register", (req, res) => {
  // tests that request body has required data
  if ("username" in req.body == false) {
    res.status(400).send("Bad Request: Missing username");
    return;
  }
  if ("email" in req.body == false) {
    res.status(400).send("Bad Request: Missing email");
    return;
  }
  if ("password" in req.body == false) {
    res.status(400).send("Bad Request: Missing password");
    return;
  }

  // Test that username is not taken
  let sql_f = "SELECT * FROM user WHERE username = '" + req.body.username + "'";
  db.query(sql_f, function(err, data, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    // If user was found with the username
    if (data.length > 0) {
      res.status(409).send("Conflict: Username already taken");
      return;
    }

    // New query to insert user into database
    let sql_i = "INSERT INTO user(username, email, password) VALUES (?)";
    const hashedPassword = bcrypt.hashSync(req.body.password, 6);
    let values = [
      req.body.username,
      req.body.email,
      hashedPassword
    ];
    db.query(sql_i, [values], function(err, data, fields) {
      if (err) {
        console.log(err);
        res.status(500).send("MySQL ERROR");
        return;
      }
      res.status(200).send({ username: req.body.username, email: req.body.email, password: hashedPassword });
    })
  })
});

app.post("/user/login", (req, res) => {
  // find user from database
  let sql_f = "SELECT * FROM user WHERE username = '" + req.body.username + "' LIMIT 1";
  db.query(sql_f, function(err, data, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    // if user not found
    if (data.length === 0) {
      res.status(401).send("Unauthorized: Username not found");
      return;
    }
  
    let dbuser = data[0];
    // compare passwords
    if (bcrypt.compareSync(req.body.password, dbuser.password) == false) {
      res.status(401).send("Unauthorized: Wrong password");
      return;
    }

    // construct body and set options
    const body = {
      id: dbuser.userid,
      username: dbuser.username,
    };

    const payload = {
      user: body,
    };

    const options = {
      expiresIn: "1209600s", // two weeks
    };

    // create and return token
    const token = jwt.sign(payload, jwtSecretKey.secret, options);
    res.status(200).send({ userId: dbuser.userid, username: dbuser.username, email: dbuser.email, token: token });
  })
});

app.delete("/user/id/:id",
  passport.authenticate("jwt", { session: false }),
  (req, res) => {
    // find user from database by id
    let sql_f = "SELECT * FROM user WHERE userid = '" + req.params.id + "' LIMIT 1";
    db.query(sql_f, function(err, data, fields) {
      if (err) {
        console.log(err);
        res.status(500).send("MySQL ERROR");
        return;
      }

      // test that user was found
      if (data.length === 0) {
        res.status(404).send("User Id Not Found");
        return;
      }

      let dbuser = data[0]
      // test that the user is authorized to modify the resource
      if (dbuser.userid !== req.user.id) {
        res.status(403).send("Forbidden: User not authorized");
        return;
      }

      // delete user from database
      let sql_d = "DELETE FROM user WHERE userid = " + req.params.id;
      db.query(sql_d, function(err, data, fields) {
        if (err) {
          console.log(err);
          res.status(500).send("MySQL ERROR");
          return;
        }

        res.status(200).send("User deleted, Id: " + req.params.id);
      })
    })
  }
);

// get users for testing
app.get("/user", (req, res) => {
  let sql = "SELECT * FROM user";
  db.query(sql, function(err, users, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    res.status(200).json({ users })
  })
});

/*********************************************
 * STORY ENDPOINTS
 ********************************************/

app.get("/story", (req, res) => {
  let number = req.query.number;
  let offset = req.query.offset;
  let sql = null;
  // Return all stories
  if(number === undefined) {
    sql = "SELECT * FROM story ORDER BY storyid DESC";
  } 
  // Return specific stories
  else {
    // Offset defaults to 0
    if(offset === undefined) { offset = 0 }

    sql = `SELECT * FROM story ORDER BY storyid DESC LIMIT ${offset}, ${number}`
  }

  db.query(sql, function(err, stories, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    res.status(200).json({ stories })
  })
  
});


// Get story by its id
app.get("/story/id/:id", (req, res) => {
  // find story from database
  let sql_f = "SELECT * FROM story WHERE storyid = " + req.params.id;
  db.query(sql_f, function(err, story, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    // if story was not found from database
    if (story.length === 0) {
      res.status(404).send("Story Id Not Found");
      return;
    }

    res.status(200).send({ story });
  })
});

// Get stories by userId
app.get("/story/userid/:userId", (req, res) => {
  // find all stories with a userId
  let sql_f = "SELECT * FROM story WHERE userid = " + req.params.userId;
  db.query(sql_f, function(err, stories, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    // if stories was not found from database
    if (stories.length === 0) {
      res.status(404).send("Stories with the userId not found");
      return;
    }

    res.status(200).send({ stories });
  })
});

// Search stories
app.get("/story/search/:keyword", (req, res) => {
  let number = req.query.number;
  let offset = req.query.offset;
  let sql = null;
  // Return all stories
  if(number === undefined) {
    sql = "SELECT * FROM story ORDER BY storyid DESC";
  } 
  // Return specific stories
  else {
    // Offset defaults to 0
    if(offset === undefined) { offset = 0 }

    sql = `SELECT * FROM story ORDER BY storyid DESC LIMIT ${offset}, ${number}`
  }

  db.query(sql, function(err, stories, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    let searchStories = [];
    let key = req.params.keyword.toLowerCase();

    stories.forEach(e => {
      let username = e.username.toLowerCase();
      let desc = e.description.toLowerCase();

      if (username.search(key) !== -1) {
        searchStories.push(e);
      } else if (desc.search(key) !== -1) {
        searchStories.push(e);
      }
    })

    res.status(200).json({ searchStories });
  })
  
});

// Get all story locations
app.get("/story/location", (req, res) => {
  let sql = "SELECT * FROM story";
  db.query(sql, function(err, stories, fields) {
    if (err) {
      console.log(err);
      res.status(500).send("MySQL ERROR");
      return;
    }

    // parse stories for only id and location data
    let locations = []
    stories.forEach(e => {
      let locObj = {
        storyId: e.storyid,
        lat: e.lat,
        lng: e.lng,
      };
      locations.push(locObj);
    });
    res.status(200).send({ locations })
  })
})

app.post("/story",
  passport.authenticate("jwt", { session: false }),
  multerUpload.single("image"),
  (req, res) => {
    if ("desc" in req.body == false) {
      res.status(400).send("Bad Request: Missing desc");
      return;
    }
    if ("lat" in req.body == false) {
      res.status(400).send("Bad Request: Missing lat");
      return;
    }
    if ("lng" in req.body == false) {
      res.status(400).send("Bad Request: Missing lng");
      return;
    }
    if ("image" in req.body == false) {
      res.status(400).send("Bad Request: Missing image");
      return;
    }

    // get datetime in correct timezone and format
    dateObj = new Date()
    localEpoch = dateObj.getTime() - (dateObj.getTimezoneOffset() * 60000);
    localDatetime = dateObj.setTime(localEpoch);
    isoDate = dateObj.toISOString();

    // add story to the database
    let sql_i = "INSERT INTO story(userid, username, description, lat, lng, timestamp, image) VALUES (?)";
    let values = [
      req.user.id,
      req.user.username,
      req.body.desc,
      req.body.lat,
      req.body.lng,
      isoDate,
      req.body.image
    ];
    db.query(sql_i, [values], function(err, data, fields) {
      if (err) {
        console.log(err);
        res.status(500).send("MySQL ERROR");
        return;
      }
      
      res.status(201).send({
        userId: req.user.id,
        username: req.user.username,
        desc: req.body.desc,
        lat: req.body.lat,
        lng: req.body.lng,
        timestamp: isoDate,
        image: req.body.image.slice(0, 30) + "..."
      });
    })
  }
);

app.delete("/story/id/:id",
  passport.authenticate("jwt", { session: false }),
  (req, res) => {

    // find story from database by id
    let sql_f = "SELECT * FROM story WHERE storyid = '" + req.params.id + "' LIMIT 1";
    db.query(sql_f, function(err, data, fields) {
      if (err) {
        console.log(err);
        res.status(500).send("MySQL ERROR");
        return;
      }

      // test that story was found
      if (data.length === 0) {
        res.status(404).send("Story Id Not Found");
        return;
      }

      let dbstory = data[0]
      // test that the user is authorized to modify the resource
      if (dbstory.userid !== req.user.id) {
        res.status(403).send("Forbidden: User not authorized");
        return;
      }

      // delete story from database
      let sql_d = "DELETE FROM story WHERE storyid = " + req.params.id;
      db.query(sql_d, function(err, data, fields) {
        if (err) {
          console.log(err);
          res.status(500).send("MySQL ERROR");
          return;
        }

        res.status(200).send("Story deleted, Id: " + req.params.id);
      })
    })
  }
);

app.listen(port, () => {
  console.log(`Example app listening at http://localhost:${port}`)
})
