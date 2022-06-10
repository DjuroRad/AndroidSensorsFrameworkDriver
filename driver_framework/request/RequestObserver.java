package driver_framework.request;

public interface RequestObserver {
    public void onRequestArrived(RequestPackage newRequest);
    public void onSubjectFinished();
}
