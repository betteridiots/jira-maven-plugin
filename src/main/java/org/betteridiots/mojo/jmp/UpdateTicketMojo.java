package org.betteridiots.mojo.jmp;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.lang.Iterable;

import java.net.URI;
import java.net.URISyntaxException;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Transition;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.domain.input.FieldInput;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.atlassian.jira.rest.client.domain.IssueFieldId;


@Mojo( name = "update-ticket", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST )
public class UpdateTicketMojo extends AbstractMojo
{

    // Maven Project
    /**
     * @parameter default-value="${project}" doesn't work
     * @required
     * @readonly
     */
    @Parameter( property = "project")
    MavenProject project;

    @Parameter( property = "settings")
    Settings settings;

    // JIRA User
    @Parameter( property = "update-ticket.user", defaultValue = "foo" )
    private String user;
   
    // JIRA Password
    @Parameter( property = "update-ticket.password", defaultValue = "foo" )
    private String password;

    // JIRA Host
    @Parameter( property = "update-ticket.host", defaultValue = "localhost" )
    private String host;

    // JIRA Issue
    @Parameter( property = "update-ticket.issue", defaultValue = "TST-1" )
    private String issue;

    // JIRA Transition ID
    @Parameter( property = "update-ticket.transition", defaultValue = "Passing" )
    private String transition;

    // JIRA Description Text
    @Parameter( property = "update-ticket.description", defaultValue = "" )
    private String description;

    // JIRA Description File
    @Parameter( property = "update-ticket.descriptionFile", defaultValue = "" )
    private String descriptionFile;

    // JIRA File Attachment Path
    @Parameter( property = "update-ticket.attachment", defaultValue = "" )
    private String attachment;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
	try {

	    final URI jiraServerUri = new URI(host);

            String pluginId = "update-ticket";
            // Inspect settings.xml for username and password if none provided as a parameter
            //if ((user == "foo") || (password == "foo")){
                getLog().info("Using creditials in settings.xml for " + pluginId);
                //Settings mavenSettings = new Settings();
                Server jiraServer = settings.getServer(pluginId);
                user = jiraServer.getUsername();
                password = jiraServer.getPassword();
	    //}

            // Log Parameters
            getLog().info( "JIRA Host is: " + host );
            getLog().info( "JIRA user is: " + user );
            getLog().info( "JIRA password is: " + password );
            getLog().info( "JIRA transition is: " + transition );
            getLog().info( "JIRA issue is: " + issue );
        
	    final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, user, password);

            final NullProgressMonitor pm = new NullProgressMonitor();
            final Issue jiraIssue = restClient.getIssueClient().getIssue(issue, pm);
            
            // transition this jiraIssue
            final Iterable<Transition> transitions = restClient.getIssueClient().getTransitions(jiraIssue.getTransitionsUri(), pm);
            final Transition jiraTransition = getTransitionByName(transitions, transition);

            if (jiraTransition == null) {
                 getLog().error("Unable to get Transition Name: "+ transition);
	    }
            
            if (descriptionFile != null) {
	            Charset charset = StandardCharsets.UTF_8;
	            try {
	        	    byte[] encoded = Files.readAllBytes(Paths.get(descriptionFile));
	        	    description = charset.decode(ByteBuffer.wrap(encoded, 0, 1024)).toString();
	            }catch(IOException e) {
	        	    getLog().error (" Caught IOException while trying to write file" + descriptionFile);
	            }
	    }

            Collection<FieldInput> fieldInputs = Arrays.asList(new FieldInput(com.atlassian.jira.rest.client.domain.IssueFieldId.DESCRIPTION_FIELD, description));
            final TransitionInput transitionInput = new TransitionInput(jiraTransition.getId(), fieldInputs);
            restClient.getIssueClient().transition(jiraIssue.getTransitionsUri(), transitionInput, pm);

            if (attachment != null) {
                    URI fileUri = new URI(attachment);
                    try {
                            File file = new File(attachment);
			    FileInputStream fileIO = new FileInputStream(file);
			    restClient.getIssueClient().addAttachment(pm, jiraIssue.getAttachmentsUri(), fileIO, file.getName()); 
		    }catch (FileNotFoundException e) {
			    getLog().error("FileNotFoundException: "+ e);
		    }
	    }


	}catch (URISyntaxException e)  {
		getLog().error("URISyntaxException: "+ e);
        } 

    }

    private Transition getTransitionByName(Iterable<Transition> transitions, String transitionName) {
	    for (Transition transition : transitions) {
		    getLog().info("Available transitions: "+ transition.getName());
		    if (transition.getName().equals(transitionName)) {
			    return transition;
		    }
	    }
	    return null;
    }
}
