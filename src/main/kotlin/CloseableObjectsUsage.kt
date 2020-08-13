object CloseableObjectsUsage {
    private val objectToUsers: HashMap<AutoCloseable, MutableSet<Any>> = hashMapOf()
    private val userToObject: HashMap<Any, MutableSet<AutoCloseable>> = hashMapOf()

    fun register(user: Any, obj: AutoCloseable) {
        objectToUsers.getOrPut(obj) { HashSet() }.add(user)
        userToObject.getOrPut(user) { HashSet() }.add(obj)
    }

    fun deregister(user: Any) {
        val objects = userToObject.remove(user) ?: return
        for (obj in objects) {
            val users = objectToUsers[obj] ?: continue
            if (users.size == 1) {
                obj.close()
                objectToUsers.remove(obj)
            } else {
                users.remove(user)
            }
        }
    }
}