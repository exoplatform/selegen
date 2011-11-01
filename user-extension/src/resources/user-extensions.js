/***************************************************************************************************************/
/** Exo User Extensions ***/
/***************************************************************************************************************/

Selenium.prototype.doGetExoExtensionVersion = function(){
	return "1.0";
};

/**
  * This function allows to enter text in a FCK field
  * Usage:
  *  - locator  the locator that points to the parent iframe (the one that contains both the toolbar and the text area)
  *  - text     the text to enter into the field
  * Run selenium rc server from maven with the userExtension parameter. 
  * 
  * For more information see the following URL:
  * - http://mojo.codehaus.org/selenium-maven-plugin/start-server-mojo.html#userExtensions
  *
  * store in exo-int/qa/selenium
  **/ 
Selenium.prototype.doTypeFCKEditor = function(locator, text) {
	    // All locator-strategies are automatically handled by "findElement"
	    var editor = this.page().findElement(locator);
	    
	    // TODO: use contentWindow instead of contentDocument for IE
	    var innerEditor = null;
	    if (editor.contentDocument)
	    	innerEditor = editor.contentDocument.getElementsByTagName("iframe")[0];
	    else if (editor.contentWindow)
	    	innerEditor = editor.contentWindow.document.getElementsByTagName("iframe")[0];
	    
	    if (innerEditor)
	      innerEditor.contentDocument.body.innerHTML = text;
	    // Replace the element text with the new text
	    // this.page().replaceText(element, valueToType);
};

/**
  * This function allows to use a specific Contextual menu
  * Usage:
  * - Locator : Element to rightclick on
  *
  * For more information see the following URL:
  * - Manually Fire event : http://www.howtocreate.co.uk/tutorials/javascript/domevents#domevld1
  * - initMouseEvent properties : http://www.quirksmode.org/js/events_properties.html
  *
  * store in exo-int/qa/selenium
  **/ 
Selenium.prototype.doComponentExoContextMenu = function(locator){
	
	var fireOnThis = this.page().findElement(locator);
    var evObj = document.createEvent('MouseEvents');
    evObj.initMouseEvent( 'mousedown', true, true, window, 1, 12, 345, 7, 220, false, false, false, false, 2, null );
    fireOnThis.dispatchEvent(evObj);

};

/**
  * This function allows to use a specific Contextual menu
  * Usage:
  * - Locator : Element to doubleclick on
	  
  * For more information see the following URL:
  * - Manually Fire event : http://www.howtocreate.co.uk/tutorials/javascript/domevents#domevld1
  * - initMouseEvent properties : http://www.quirksmode.org/js/events_properties.html
  * 
  * store in exo-int/qa/selenium
  **/ 
Selenium.prototype.doComponentExoDoubleClick = function(locator){
	
	var fireOnThis = this.page().findElement(locator);
    var evObj = document.createEvent('MouseEvents');
    evObj.initMouseEvent('dblclick', true, true, window, 1, 12, 345, 7, 220, false, false, false, false,0, null );
    fireOnThis.dispatchEvent(evObj);

};

//-----------------write by linh_vu
Selenium.prototype.doTypeRepeated = function(locator, text) {
    // All locator-strategies are automatically handled by "findElement"
    var element = this.page().findElement(locator);

    // Create the text to type
    var valueToType = text + "__" +text;

    // Replace the element text with the new text
    this.page().replaceText(element, valueToType);
};
//--------------------------------
Selenium.prototype.doTypeRandom = function(locator,string_length) {

	var element = this.page().findElement(locator);

        var chars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        var randomstring = '';
        for (var i=0; i<string_length; i++) {
            var rnum = Math.floor(Math.random() * chars.length);
            randomstring += chars.substring(rnum,rnum+1);
        }
        //return randomstring;
		var valueToType =randomstring;

		 this.page().replaceText(element, valueToType);
    }
 //-------------------------------
 Selenium.prototype.doTypeRandomEmail = function randomString(locator,string_length) {

	var element = this.page().findElement(locator);

        var chars = "abcdefghiklmnopqrstuvwxyz";
        var randomstring = '';
        for (var i=0; i<string_length; i++) {
            var rnum = Math.floor(Math.random() * chars.length);
            randomstring += chars.substring(rnum,rnum+1);
        }
        //return randomstring;
		var valueToType =randomstring+"@exoplatform.com";

		 this.page().replaceText(element, valueToType);
    }

/**
 * storeValue, storeText, storeAttribute and store actions now 
 * have 'global' equivalents.
 * Use storeValueGlobal, storeTextGlobal, storeAttributeGlobal or storeGlobal
 * will store the variable globally, making it available it subsequent tests.
 *
 * See the Reference.html for storeValue, storeText, storeAttribute and store
 * for the arguments you should send to the new Global functions.
 *
 * example of use
 * in testA.html:
 * +------------------+----------------------+----------------------+
 * |storeGlobal       | http://localhost/    | baseURL              |
 * +------------------+----------------------+----------------------+
 * 
 * in textB.html (executed after testA.html):
 * +------------------+-----------------------+--+
 * |open              | ${baseURL}Main.jsp    |  |
 * +------------------+-----------------------+--+
 *
 * Note: Selenium.prototype.replaceVariables from selenium-api.js has been replaced
 *       here to make it use global variables if no local variable is found.
 *       This might cause issues if you upgraded Selenium in the future and this function 
 *       has been changed.
 *
 * @author Guillaume Boudreau
 */
 
globalStoredVars = new Object();

/*
 * Globally store the value of a form input in a variable
 */
Selenium.prototype.doStoreValueGlobal = function(target, varName) {
    if (!varName) {
        // Backward compatibility mode: read the ENTIRE text of the page
        // and stores it in a variable with the name of the target
        value = this.page().bodyText();
        globalStoredVars[target] = value;
        return;
    }
    var element = this.page().findElement(target);
    globalStoredVars[varName] = getInputValue(element);
};

/*
 * Globally store the text of an element in a variable
 */
Selenium.prototype.doStoreTextGlobal = function(target, varName) {
    var element = this.page().findElement(target);
    globalStoredVars[varName] = getText(element);
};

/*
 * Globally store the value of an element attribute in a variable
 */
Selenium.prototype.doStoreAttributeGlobal = function(target, varName) {
    globalStoredVars[varName] = this.page().findAttribute(target);
};

/*
 * Globally store the result of a literal value
 */
Selenium.prototype.doStoreGlobal = function(value, varName) {
    globalStoredVars[varName] = value;
};

/*
 * Search through str and replace all variable references ${varName} with their
 * value in storedVars (or globalStoredVars).
 */
Selenium.prototype.replaceVariables = function(str) {
    var stringResult = str;

    // Find all of the matching variable references
    var match = stringResult.match(/\$\{\w+\}/g);
    if (!match) {
        return stringResult;
    }

    // For each match, lookup the variable value, and replace if found
    for (var i = 0; match && i < match.length; i++) {
        var variable = match[i]; // The replacement variable, with ${}
        var name = variable.substring(2, variable.length - 1); // The replacement variable without ${}
        var replacement = storedVars[name];
        if (replacement != undefined) {
            stringResult = stringResult.replace(variable, replacement);
        }
        var replacement = globalStoredVars[name];
        if (replacement != undefined) {
            stringResult = stringResult.replace(variable, replacement);
        }
    }
    return stringResult;
};
