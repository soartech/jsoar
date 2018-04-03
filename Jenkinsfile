pipeline {
	agent any
	tools {
		maven 'Maven 3.5.0'
		jdk 'JDK 8'
	}
	stages {
			stage('Setup and Checkout') {
			steps {
				// This checks out the same source as used in the multibranch scan
				checkout poll: true, changelog: true, scm: [$class: 'GitSCM', branches: [[name: '*/${BRANCH_NAME}']]]
			}
		}

			stage('Maven Build') {
			steps {
					withMaven(maven: 'Maven 3.5.0') {
						// Run the maven build
						sh "mvn clean package pmd:pmd pmd:cpd findbugs:findbugs"
				}
			}
		}
	
		stage('Analysis') {
			steps {
				warnings consoleParsers: [[parserName: 'Maven'], [parserName: 'Java Compiler (javac)']], shouldDetectModules: true
				openTasks canComputeNew: true
				findbugs canComputeNew: true pattern: '**/target/findbugsXml.xml'
				pmd canComputeNew: true
				dry canComputeNew: true			
				step([$class: 'AnalysisPublisher', canComputeNew: true])
			}
		}
	}

	post {
		failure {
			script {
				// Email cyber@ for broken master branch, else just the culprits
				GIT_BRANCH = 'origin/' + BRANCH_NAME
				if (GIT_BRANCH == 'origin/master') {
					step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: 'cyber@soartech.com', sendToIndividuals: true])
				}
				else {
					step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider']])
, sendToIndividuals: true])
				}
			}
		}
	}
}
