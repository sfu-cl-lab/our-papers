# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Get handles on the database and object attributes.

currentAttrs = prox.objectAttrs

# Define the new object attribute name.

newAttrName = "isStudent"

# Define the function that determines the new attribute value.
# This is a switch function. It sets the value of the new attribute
# to 1 if the object's "pagetype" attribute is equal to "Student"
# and 0 otherwise.

newAttrFunction = "pagetype = \"Student\" ==> 1, default ==> 0"

# See if this object attribute already exists in the database.
# If it does, ask the user whether to delete it so we can
# re-create it.

prompt = "Attribute already exists. Delete and recreate?"

if currentAttrs.isAttributeDefined(newAttrName):
   deleteExisting = prox.getYesNoFromUser(prompt)
   if deleteExisting:
      currentAttrs.deleteAttribute(newAttrName)
      print "Creating new ", newAttrName, " attribute"
      prox.addAttribute(currentAttrs,newAttrName,newAttrFunction)
else:
   print "Creating new ", newAttrName, " attribute"
   prox.addAttribute(currentAttrs,newAttrName,newAttrFunction)
