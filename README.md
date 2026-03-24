Consists of two "projects", the Red-Black Tree itself done in Java, and the RBT visualiser done in Python.
There are a few reasons for this decision, but you may already know why if you're here in the first place.

The Red-Black Tree project in Java has more than just the RBT, it has a CSV reader that ONLY reads the NASA Planetary Systems table (included in the project).
The table was exported from their website, find it here: https://exoplanetarchive.ipac.caltech.edu/index.html

Next, the Planet script breaks down the CSV so that it can later be used in the JSON exporter script.
With this, it will be used in our Python visualiser script to view our built RBT.

There are comments left inside the Main Java script and on the Python script on how to execute them.
As it says in the Main file, compile the project first. Then, use the absolute path to read the CSV file and put quotes around when using the command in a terminal.

For the Python script, make sure both the exported JSON and the script are in the same directory.
The file path read is HARDCODED, so make sure the file name is equal to the file path Python is reading. Then, just simply run the program.
