
function runAgentCommand(url, command)
{
	$.ajax({ 
        url: url, 
        type: "POST", 
        dataType: "text",
        data: "command=" + command
    });
}
