if (typeof window.URL.createObjectURL === 'undefined') {
  window.URL.createObjectURL = (): string => {
    // Do nothing
    // Mock this function for mapbox-gl to work
    return "";
  };
}

// @ts-ignore
class Worker {

  private readonly url;
  private readonly onmessage: () => void;
  constructor(stringUrl) {
    this.url = stringUrl;
    this.onmessage = () => {};
  }

  postMessage(_msg) {
    this.onmessage();
  }
}window.Worker = Worker;
