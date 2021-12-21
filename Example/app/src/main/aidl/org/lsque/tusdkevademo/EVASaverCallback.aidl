// EVASaverCallback.aidl
package org.lsque.tusdkevademo;

// Declare any non-default types here with import statements

interface EVASaverCallback {

void progress(double p,String taskId);
void saveSuccess(String taskId);
void saveFailure(String taskId);

}