// @ts-check
import {
    ECSClient,
    DescribeTaskDefinitionCommand,
    RegisterTaskDefinitionCommand,
    UpdateServiceCommand,
    RegisterTaskDefinitionCommandInput,
    ListServicesCommand,
    DescribeServicesCommand,
    ListServicesCommandOutput, ListClustersCommand
} from "@aws-sdk/client-ecs";

import { Context, EventBridgeEvent, Handler  } from "aws-lambda";

export const handler = async (
  event: EventBridgeEvent<any, any>,
  context: Context
): Promise<object> => {
    console.log(event);
    let response: any = "";
    //check this is an event for this lambda
    if(process.env.PREFIX) {
        if (event.detail.name.substring(0, process.env.PREFIX.length) == process.env.PREFIX) {

            const myRegion: string = "eu-west-2";
            const ecsClient = new ECSClient({apiVersion: "2014-11-13", region: myRegion});
            //get the list of clusters
            const listClustersParams = {};
            const listClustersCommand = new ListClustersCommand(listClustersParams);
            const listClustersResult = await ecsClient.send(listClustersCommand);
            console.log("listClustersResult: ");
            console.log(listClustersResult);
            if (listClustersResult.clusterArns) {
                for (const clusterArn of listClustersResult.clusterArns) {
                    console.log("clusterArn: ");
                    console.log(clusterArn);
                    //get the list of services:
                    const listServicesParams = {
                        cluster: clusterArn
                    };
                    const listServicesCommand = new ListServicesCommand(listServicesParams);
                    console.log("Pre await");


                    const listOfServices: ListServicesCommandOutput = await ecsClient.send(listServicesCommand);
                    console.log("listOfServices: ");
                    console.log(listOfServices);
                    if (listOfServices.serviceArns) {
                        console.log("listOfServices: " + listOfServices.serviceArns);
                        for (const serviceArn of listOfServices.serviceArns) {
                            console.log("serviceArn: ");
                            console.log(serviceArn);
                            //get the service:
                            const params = {
                                cluster: clusterArn,
                                services: [serviceArn]
                            };
                            const describeServicesCommand = new DescribeServicesCommand(params);
                            const servicesDesc = await ecsClient.send(describeServicesCommand);
                            if (servicesDesc.services) {
                                console.log(servicesDesc.services);
                                for (const serviceDefn of servicesDesc.services) {
                                    console.log("serviceDefn: ");
                                    console.log(serviceDefn);
                                    //const containerIndex = servicesDesc.services.indexOf(serviceDefn);
                                    //get the task definition of the service
                                    let doUpdate: boolean = false;
                                    const descParams = {
                                        taskDefinition: serviceDefn.taskDefinition
                                    };
                                    const describeTaskDefinitionCommand = new DescribeTaskDefinitionCommand(descParams);
                                    let taskDefnDesc = await ecsClient.send(describeTaskDefinitionCommand);
                                    console.log('taskDefnDesc: ');
                                    console.log(taskDefnDesc);
                                    if (taskDefnDesc.taskDefinition) {
                                        if (taskDefnDesc.taskDefinition.containerDefinitions) {
                                            for (const containerDefn of taskDefnDesc.taskDefinition.containerDefinitions) {
                                                console.log("containerDefns: ");
                                                console.log(containerDefn);
                                                if (containerDefn.environment) {
                                                    containerDefn.environment.forEach(function (environmentVar) {
                                                        //see if this param has an env var with this event:
                                                        // @ts-ignore
                                                        if (event.detail.name.substring(process.env.PREFIX.length) == environmentVar.name) {
                                                            console.log("environmentVar matched!!: ");
                                                            console.log(environmentVar.name);
                                                            doUpdate = true;
                                                            environmentVar.value = event.detail.description;
                                                        }
                                                    })
                                                }
                                            }
                                            if (doUpdate) {
                                                console.log("taskDefnDesc: ");
                                                console.log(taskDefnDesc);
                                                //register new definition:
                                                //taskDefnDesc.taskDefinition.family =
                                                //const registerTaskDefinitionCommandInput = new RegisterTaskDefinitionCommandInput
                                                const registerTaskDefinitionCommand = new RegisterTaskDefinitionCommand(<RegisterTaskDefinitionCommandInput>taskDefnDesc.taskDefinition);
                                                const registerTaskDefnResponse = await ecsClient.send(registerTaskDefinitionCommand);
                                                if (registerTaskDefnResponse) {
                                                    console.log("registerTaskDefnResponse: " + registerTaskDefnResponse);
                                                    if (registerTaskDefnResponse.taskDefinition) {
                                                        //apply new definition:

                                                        const updateParams = {
                                                            cluster: clusterArn,
                                                            service: serviceDefn.serviceName,
                                                            taskDefinition: registerTaskDefnResponse.taskDefinition.taskDefinitionArn
                                                        };
                                                        const updateServiceCommand = new UpdateServiceCommand(updateParams);
                                                        const updateServiceResponse = await ecsClient.send(updateServiceCommand);

                                                        if (updateServiceResponse) {
                                                            response = {
                                                                statusCode: 200,
                                                                headers: {
                                                                    "Content-Type": "text/plain"
                                                                },
                                                                body: `testing... updateServiceResponse`
                                                            }
                                                        } else {
                                                            response = {
                                                                statusCode: 200,
                                                                headers: {
                                                                    "Content-Type": "text/plain"
                                                                },
                                                                body: `testing... no updateServiceResponse`
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            response = {
                statusCode: 200,
                headers: {
                    "Content-Type": "text/plain"
                },
                body: `testing...last else`
            }
        }
    }
    return response;
}