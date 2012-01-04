<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<form method="post" action="<%= request.getAttribute("credentialValidateUrl") %>">
	<input type="hidden" name="lt" value="<%= request.getAttribute("loginToken") %>" />
	<input type="hidden" name="on_error" value="<c:out value="${requestScope.onErrorUrl}" />" />
	
	<c:if test="${requestScope.error}">
		<div style="color:red">Invalid username or password.</div>
	</c:if>
	
	<label for="<portlet:namespace/>username">Username:</label>
	<input type="text" name="username" id="<portlet:namespace/>username" size="10" />
	<label for="<portlet:namespace/>password">Password:</label>
	<input type="password" name="password" id="<portlet:namespace/>password" size="10" />

	<div>
		<input type="checkbox" name="rememberMe" value="1"
			id="<portlet:namespace/>rememberme" /> 
			<label for="<portlet:namespace/>rememberme">Remember Me</label>
	</div>


<input type="submit" value="Login" />
</form>