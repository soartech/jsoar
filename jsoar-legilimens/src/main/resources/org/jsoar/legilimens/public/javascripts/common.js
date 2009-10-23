
var Legilimens = function() {
	return {
		runAgentCommand : function(command)
		{
			$.ajax({ 
		        url: this.commands_url, 
		        type: "POST", 
		        dataType: "text",
		        data: "command=" + command,
		        success: function(data, textStatus) {
					//$('#flash').html("Executed '" + command + "'").show();
					if(Legilimens.afterRun != null) {
						Legilimens.afterRun();
					}
				},
				error: function(data, textStatus) {
					if(Legilimens.afterRun != null) {
						Legilimens.afterRun();
					}
				}
		    });
		},
		afterRun : null,

		run : function() { this.runAgentCommand("run"); },
		step : function() { this.runAgentCommand("step"); },
		stop : function() { this.runAgentCommand("stop"); },
		initSoar : function() { this.runAgentCommand("init-soar"); },
		source : function(url) { this.runAgentCommand("source {" + url + "}"); },
		
		excise : function(url) {
			$.ajax({
				url: url + "?method=DELETE",
				type: "POST",
				success: function(data, textStatus) {
					window.location=Legilimens.productions_url
				},
				error: function(data, textStatus) {
					window.location=Legilimens.productions_url
				}
			})
		}
		
	};
}();