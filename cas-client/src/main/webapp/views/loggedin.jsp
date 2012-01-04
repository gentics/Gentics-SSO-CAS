<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
Successfully logged in as <%= request.getUserPrincipal().getName() %>.
<a href="${ logoutUrl }">Logout</a>.