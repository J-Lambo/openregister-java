# Create your own register locally

You can use this script to run your own register locally and experiment with its configuration.

Please note that this script is experimental and has not been fully tested. The outputs and functionality are likely to change as the registers team learns more about the needs of users creating their own registers.

If you'd like to be involved in user research and testing for this in future, please [contact the GDS registers team](https://registers.cloudapps.digital/support.html).

## Before you start

You'll need:
* Docker
* Python3
* curl
* An understanding of the [register APIs](https://registers-docs.cloudapps.digital/)

This iteration of the script is pre-configured to spin up a register of schools based on the [School eng register](https://school-eng.alpha.openregister.org/), which is currently in alpha.

It also uses "basic registers" such as the [register register](https://register.register.gov.uk/), [field register](https://field.register.gov.uk/).

You can amend these details once you have a copy of the School eng register running locally. You can find credential information in the configuration files.

## Run register locally

Run `./run-application.sh` to spin up a local copy of `openregister-java` using Docker.

The command will create Docker containers that will:
* build the application from source
* start and configure the database
* run the "basic" registers, such as the Register Register and Field Register with `config.register.basic.yaml`
* run a register configured with `config.docker.register.yaml`

The basic registers will be cloned from a specific phase, such as alpha. You can see them locally at `*.local.openregister.org:8081`. For example, `field.local.openregister.org:8081`.

You should now see the School eng register locally at `127.0.0.1:8080`.

If you want to amend this register, you can:
* create a new register
* change the configuration

## Create a new register

To create a new register, you first need to create a new entry in the register register.

Choose a primary key for your register. This will also be the name of your register. For example, foobar.

Add the name to the field register using:
`$ echo '[{"field":"foobar", "datatype": "string", "phase": "alpha", "cardinality": "1", "text": "test field"}]' | python3 ./scripts/json-to-rsf/json2rsf.py field | curl field.local.openregister.org:8081/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar`

Then, you'll need to create an entry in the register register using:
`$ echo '[{"phase":"alpha","registry":"government-digital-service","text":"A test register","fields":["foobar", "name","start-date","end-date"],"register":"foobar"}]' | python3 ./scripts/json-to-rsf/json2rsf.py register | curl register.local.openregister.org:8081/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar`

Check this is successful at `field.local.openregister.org:8081/records` and `register.local.openregister.org:8081/records`. You should see the name of your register, such as `foobar`, as the first record. 

Now you can update your local register to use this.

## Change the configuration

First, you'll need to change the configuration.

Go to the `config.docker.register.yaml` file.

Change `register: school-eng` to the name of your register, for example `register: foobar`.

Update the schema in the same way, for example: `schema: foobar`.

Restart your docker container:
`docker restart openregister-register`

You should see your new empty register at `127.0.0.1:8080`.

## Load data into your register

You can now load your own data in the new register.

For example:

```
$ echo '[{"foobar": "a", "name": "something"}]' | python3 ./scripts/json-to-rsf/json2rsf.py foobar | curl 127.0.0.1:8080/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
$ echo '[{"foobar": "b", "name": "something else"}]' | python3 ./scripts/json-to-rsf/json2rsf.py foobar | curl 127.0.0.1:8080/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
$ echo '[{"foobar": "c", "name": "another thing"}]' | python3 ./scripts/json-to-rsf/json2rsf.py foobar | curl 127.0.0.1:8080/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
```

## Contact and support

The GDS registers team provides operational support from 09:00 - 17:00 Monday-Friday.

[Contact the team](https://registers.cloudapps.digital/support.html) if you have any problems or questions that are not covered in this guide.
