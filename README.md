Consists of two "projects", the Red-Black Tree itself done in Java, and the RBT visualiser done in Python.
Was there any reason to do them in different languages? Probably not, other than maybe some performance differences.

The Red-Black Tree project in Java has more than just the RBT, it has a CSV reader that ONLY reads the NASA Planetary Systems table (included in the project).
The table was exported from their website, find it here: https://exoplanetarchive.ipac.caltech.edu/index.html

Next, the Planet script breaks down the CSV so that it can later be used in the JSON exporter script.
With this, it will be used in our Python visualiser script to view our built RBT.

This was painful.
