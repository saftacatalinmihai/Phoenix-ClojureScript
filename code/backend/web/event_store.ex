defmodule Backend.EventStore do

 use GenServer

  def start_link do
    GenServer.start_link(__MODULE__, :ok, name: EventStore)
  end

  def get_events do 
    GenServer.call(__MODULE__, :get_events)
  end

  def init(:ok) do
    {:ok, []}
  end

  def handle_call(:get_events, from, ev_list) do
    {:reply, ev_list, ev_list}
  end

  def handle_cast(event, ev_list) do
    IO.inspect event
    {:noreply, [event | ev_list]}
  end

end
