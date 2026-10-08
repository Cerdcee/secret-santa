# Secret Santa solver

Author : Cerdcee

Licence : Attribution-NonCommercial-ShareAlike 4.0 International, see `LICENCE.txt`

---

## Description

This aims to be a (relatively) simple [Secret Santa](https://en.wikipedia.org/wiki/Secret_Santa) picker that you can run 
from your computer without the need to register to any online service. The only thing you need is 

The project is made so that even the organiser is not aware of who gifts to whom, so they can participate too !

You can choose the number of gifts given. You can also describe constraints to force a person to be (or NOT be) 
assigned to another, or several others, and combine these constraints. 

In case anyone has lost their email, a backup file with all the information is created at runtime. 

---

## How to use

- Fill `/src/main/resources/email.properties` with the info of the SMTP email server and account you will be sending the 
emails from.
  - To get the `password` for Gmail, I used the "application password", see [here](https://myaccount.google.com/apppasswords)
  to set one up.
- Replace the content of `/src/main/resources/example.json` with your list of people. 
  - Alternatively, you can create a new file. Remember to change the `filename` variable in `/src/main/kotlin/Main.kt`.
  - For each person, you will need a name and a valid email address. You can also describe constraints !
- Open `/src/main/kotlin/Main.kt` and change `nbGiftsPerPerson` to determine how many gifts each person will give (and receive). Default 
is 1.
  - Whatever the value of `nbGiftsPerPerson`, someone cannot give more than one gift to a specific person. 
- Run the `main()` function in `/src/main/kotlin/Main.kt`.
  - The emails will be automatically sent.
  - The list of who gifts to whom is saved in the `backupFilename` file, in case of need. If you participate in the event, 
do not open it !


### Describing people's requests/constraints

In the `/src/main/resources/example.json` file, you have an example of what format is expected to describe the people. 
It is a list of objects with the following properties : 
```json
{
    "id": "alice",
    "name": "Alice",
    "email": "alice@test.com",
    "requests": [
      {
        "type": "NO_GIFT_TO",
        "otherPersonId": "bob"
      },
      {
        "type": "GIFT_TO",
        "otherPersonId": "charles"
      }
    ]
  }
```
- The `id` can be anything, but keep it simple and UNIQUE. It is used to describe requests.
- The `name` is used to address the person in the email.
- Make sure the `email` is a valid email address.
- The `requests` array is used to describe the constraints.
  - It can be empty but has to be present.
  - It has no size limit, but keep in mind that all constraints MUST be possible to solve at the same time for the 
program to work. Also, the more constraints, the longer the program may take to compute. 
  - Constraint types : 
    - `GIFT_TO` : ensure the person will give one gift to a specific person (described by their id). Useful if someone 
    has already bought a gift ! If only one gift is given, it will be to that person. If several gifts are given, one 
    among them will be given to that person.
    - `NO_GIFT_TO` : ensure the person will NOT give a gift to someone (described by their id). Useful if you do not 
    want people who are a couple to give gifts to each other for instance.
    - You can combine several of each for one person, as long as the number of gifts given by one person and the
    combination of contraints for all people makes it possible to solve.

### Peek into the backup file

The project is made so that even the organiser is not aware of the pairings between people. That being said, it happens
that a person has lost the received email and needs to be reminded of the people who are assigned to them.

There is a bash script in the project that displays the people assigned to a given person id. From the project root 
folder, rename or copy the backup file to name it `secret_santa.backup` and then run : 

```shell
./findInBackup.sh <person id, the one from example.json>
```

### Customizing the email

The content of the email is defined in the `src/main/resources/template/secret_santa.html` file. 
The `src/main/resources/template/secret_santa.txt` file has only the text content. It is the fallback version in case 
the recipient's email server has an issue displaying the HTML. 

/!\ For now the text is in French so you may need to translate it before you use run the program !

- If you change the text, keep it the same in both files (HTML and text).
- Use the [mustache templating format](https://en.wikipedia.org/wiki/Mustache_(template_system)) for the variables.
- The variable names as well as the subject of the email can be found in `/src/main/kotlin/email/EmailService.kt` in the 
`sendEmail()` function.

---

## TODO

- Alternative `PeopleSortingService` because `SortingSatSolverService` takes too much time and always gives results in 
the same order.
- Average computing time with `SortingSatSolverService` and each other sorting service
- Look for another faster sat solver implementation
- Read backup file to send the emails again (so the overseer using this program does not have to look at the backup file
if they are part of the event)
  - May need to change the format of the backup file to json
  - Make it so the emails can be sent to all or only to a list of people
- Handle complex OR constraints over several people (ex: A OR B must gift to C; A must gift to B OR C)
  - May need to change the structure of the input json file to separate the constraints from the people.
- Group all the code in `logic` to build and transform logical expressions in a separate library
- UI ?
- Allow people to publish gift ideas or ask for gift ideas
  - Very unlikely to be possible given the implementation of the project (no database, answering the organiser email would
  not easily serve this purpose and it is not supposed to be answered)
- Translation of the email template