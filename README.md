# FLF Admin App

This app allows for easy editing of the site content. Currently supported:

- Adding a new horse listing from a Facebook post
- Adding a new horse listing by filling out the horse info
- Marking a horse as placed

# Installing

On Windows:

Register on Github.com, and get Katherine to grant you access to the site
Download and unzip "install.zip"
Create a Github Personal Access Token (classic) on Github.com
Add that token to the env file

Now you can run the app by double-clicking it like a normal app

On Mac:
Not really supported easily right now - clone the repo, then run the same authorization steps as on windows, then from the terminal, run `chmod a+rwx gradlew; ./gradlew run`

If someone needs this on a Mac, we can investigate other options, but the requirement that dmg files be signed by a registered Mac developer likely means you'll be messing with the command line anyway

# Using The App

Hopefully self explanatory from the menu buttons. Each task saves data to the cloud, so you can, for example, mark a horse as placed, then exit, and when you come back, they will still be marked as placed.

 Once you've made all the changes you want for one session, click 'Deploy Changes To Site' - you can preview the changes before they go live 

# If a change shows up as broken

Right now: grab the staging branch and fix the html

Soon: There will be an 'edit' tab

