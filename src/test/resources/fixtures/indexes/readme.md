* Local authorities by start-date example

This example initially adds entries to the register using local-authority-type as the key (this is not included as part of the item definitions).

There are three distinct items added by the five entries:
    - Notts - sha-256:768ccbd7702169778bcc43abe57cd834b977fdef887567c637b296a6e379b4e3, start-date: 1990
    - London - sha-256:f2ad63acbcff98050b0ba1fe708f33711dee1cce8931ba876f0cf2ad6d0225a0, start-date: 1880
    - Leics - sha-256:3300687af84cc4c365407484f93848695a0d82b0918c894e56d2d49ab93ba27c, start-date: 1770

As these items all have different `start-date`s, the index will contain three entries, with keys 1990, 1880 and 1770.

The corresponding index table looks as follows:
name		key		hash																sen		een		sien	eien
start-date	1990	768ccbd7702169778bcc43abe57cd834b977fdef887567c637b296a6e379b4e3	1				1
start-date	1880	768ccbd7702169778bcc43abe57cd834b977fdef887567c637b296a6e379b4e3	2		5		2		2
start-date	1770	768ccbd7702169778bcc43abe57cd834b977fdef887567c637b296a6e379b4e3	3				3
start-date	1880	768ccbd7702169778bcc43abe57cd834b977fdef887567c637b296a6e379b4e3	4				2

Note that when entry number 4 is appended to the register, we use the sien of 2, rather than leaving this column null.



