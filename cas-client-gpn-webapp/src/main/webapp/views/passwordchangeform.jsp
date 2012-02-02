<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<p>Welcome <%= request.getAttribute("username") %> - Please change your password.</p>
	<c:if test="${requestScope.error}">
		<div style="color:red">Passwords do not match.</div>
	</c:if>

<form method="post" action="<%= request.getAttribute("actionurl") %>">
	<label for="<portlet:namespace/>newpassword">New Password:</label>
	<input type="password" name="newpassword" id="<portlet:namespace/>newpassword" size="10" />
	
	<label for="<portlet:namespace/>repassword">Retype Password:</label>
	<input type="password" name="repassword" id="<portlet:namespace/>repassword" size="10" />
	
	<input type="submit" value="Change Password" />
</form>

