package org.betteridiots.mojo.jmp;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import java.io.File;

public class UpdateTicketMojoTest extends AbstractMojoTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    public void testMojoGoal() throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/jira-maven-test/jira-maven-test.xml" );

        UpdateTicketMojo mojo = (UpdateTicketMojo) lookupMojo( "update-ticket", testPom );

        // Check for null values
        assertNotNull( mojo );
        assertNotNull( getVariableValueFromObject( mojo, "host" ) );
//        assertNotNull( getVariableValueFromObject( mojo, "user" ) );
//        assertNotNull( getVariableValueFromObject( mojo, "password" ) );

        // Execute mojo test
//        mojo.execute();
    }
}
