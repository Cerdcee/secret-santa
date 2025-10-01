# Secret Santa solver

## How to use

- Fill `email.properties` with the info of the email server and account you will be sending the emails from.
- Replace the content of `example.json` with your list of people. 
  - Alternatively, you can create a new file. Remember to change the `filename` variable in `Main.kt`.
  - For each person, you will need a name and a valid email address. You can also describe constraints !
- Open `Main.kt` and change `nbGiftsPerPerson` if necessary (default is 1).
  - Whatever the value of `nbGiftsPerPerson`, someone cannot give more than one gift to a specific person. 
- Run the `main()` function in `Main.kt`.
  - The emails will be automatically sent.
  - The list of who gifts to who is saved in the `backupFilename` file, in case of need. If you participate in the event, 
do not open it !

/!\ Average computing time with `SortingSatSolverService` : TODO

### Describing people's requests/constraints

In the `example.json` file, you have an example of what format is expected to describe the people. It is a list of 
objects with the following properties : 
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
        "otherPersonId": "bob"
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
    - `GIFT_TO` : ensure the person will give one gift to someone (described by their id). Useful if someone has already 
bought a gift ! If only one gift is given, it will be to that person. If several gifts are given, one among them will be 
given to that person.
    - `NO_GIFT_TO` : ensure the person will NOT give a gift to someone (described by their id). Useful if you do not 
want people who are a couple to give gifts to each other for instance.
    - You can combine several of each for one person, as long as the number of gifts given by one person makes it 
possible to solve.


# TODO
- Alternative `PeopleSortingService` because `SortingSatSolverService` takes too much time and always gives results in 
the same order
- Look for another faster sat solver
- Read backup file to send the emails again (so the overseer using this program does not have to look at the backup file
if they are part of the event)
  - May need to change the format of the backup file to json
- Handle complex OR constraints over several people (ex: A OR B must gift to C; A must gift to B OR C)
  - May need to change the structure of the input json file to separate the constraints from the people.
- Group all the code in `logic` to build and transform logical expressions in a separate library